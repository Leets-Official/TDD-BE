package com.leets.tdd.board.service;

import com.leets.tdd.board.domain.Comment;
import com.leets.tdd.board.domain.Post;
import com.leets.tdd.board.dto.CommentCreateRequest;
import com.leets.tdd.board.repository.CommentRepository;
import com.leets.tdd.board.repository.PostRepository;
import com.leets.tdd.global.webpush.WebPushPayload;
import com.leets.tdd.global.webpush.WebPushSender;
import com.leets.tdd.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebPushSender webPushSender;

    @InjectMocks
    private PostService postService;

    private static final Long POST_ID = 1L;
    private static final Long POST_AUTHOR_ID = 10L;   // 원글 작성자
    private static final Long COMMENTER_ID = 20L;     // 댓글 작성자(다른 사람)

    private Post postBy(Long authorId) {
        return Post.create(authorId, "제목", "내용");
    }

    // save가 id를 가진 Comment를 반환하도록 세팅 (createComment가 .getId()를 호출함)
    private void stubCommentSave() {
        Comment saved = Comment.create(POST_ID, COMMENTER_ID, null, "댓글내용");
        ReflectionTestUtils.setField(saved, "id", 100L);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);
    }

    @Test
    @DisplayName("다른 사람 글에 댓글을 달면 원글 작성자에게 BOARD 알림을 발송한다")
    void createComment_othersPost_sendsToAuthor() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(postBy(POST_AUTHOR_ID)));
        stubCommentSave();

        postService.createComment(POST_ID, COMMENTER_ID, new CommentCreateRequest("댓글내용", null));

        ArgumentCaptor<WebPushPayload> captor = ArgumentCaptor.forClass(WebPushPayload.class);
        verify(webPushSender, times(1)).sendToUser(eq(POST_AUTHOR_ID), captor.capture());
        WebPushPayload payload = captor.getValue();
        Assertions.assertThat(payload.category()).isEqualTo("BOARD");
        Assertions.assertThat(payload.url()).isEqualTo("/posts/" + POST_ID);
    }

    @Test
    @DisplayName("자기 글에 자기가 댓글을 달면 알림을 발송하지 않는다")
    void createComment_ownPost_skips() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(postBy(POST_AUTHOR_ID)));
        stubCommentSave();

        postService.createComment(POST_ID, POST_AUTHOR_ID, new CommentCreateRequest("댓글내용", null));

        verify(webPushSender, never()).sendToUser(any(), any(WebPushPayload.class));
    }

    @Test
    @DisplayName("대댓글을 달아도 원글 작성자에게 알림을 발송한다")
    void createComment_reply_sendsToAuthor() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(postBy(POST_AUTHOR_ID)));
        // 부모 댓글 (일반 댓글) - 대댓글 검증 통과용
        Comment parent = Comment.create(POST_ID, 30L, null, "부모댓글");
        ReflectionTestUtils.setField(parent, "id", 50L);
        when(commentRepository.findById(50L)).thenReturn(Optional.of(parent));
        stubCommentSave();

        postService.createComment(POST_ID, COMMENTER_ID, new CommentCreateRequest("대댓글내용", 50L));

        verify(webPushSender, times(1)).sendToUser(eq(POST_AUTHOR_ID), any(WebPushPayload.class));
    }

}
