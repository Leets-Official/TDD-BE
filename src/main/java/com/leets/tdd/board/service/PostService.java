package com.leets.tdd.board.service;

import com.leets.tdd.global.webpush.WebPushSender;
import com.leets.tdd.global.webpush.WebPushPayload;
import com.leets.tdd.board.domain.Comment;
import com.leets.tdd.board.domain.Post;
import com.leets.tdd.board.dto.CommentCount;
import com.leets.tdd.board.dto.CommentCreateRequest;
import com.leets.tdd.board.dto.CommentResponse;
import com.leets.tdd.board.dto.PostCreateRequest;
import com.leets.tdd.board.dto.PostDetailResponse;
import com.leets.tdd.board.dto.PostListResponse;
import com.leets.tdd.board.repository.CommentRepository;
import com.leets.tdd.board.repository.PostRepository;
import com.leets.tdd.global.error.CommonErrorCode;
import com.leets.tdd.global.error.CustomException;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_COMMENT_SIZE = 500;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final WebPushSender webPushSender;

    /** 게시글을 작성한다. */
    @Transactional
    public Long createPost(Long userId, PostCreateRequest request) {
        Post post = Post.create(userId, request.title(), request.content());
        return postRepository.save(post).getId();
    }

    /** 게시글 목록을 최신순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<PostListResponse> getPosts(int size) {
        int boundedSize = Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(0, boundedSize);

        List<Post> posts = postRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        if (posts.isEmpty()) {
            return List.of();
        }

        Map<Long, String> nicknames = findNicknames(posts.stream().map(Post::getUserId).toList());
        Map<Long, Long> commentCounts = findCommentCounts(posts.stream().map(Post::getId).toList());

        return posts.stream()
                .map(post -> PostListResponse.of(
                        post,
                        nicknames.get(post.getUserId()),
                        commentCounts.getOrDefault(post.getId(), 0L)
                ))
                .toList();
    }

    /** 게시글 상세를 조회한다. */
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = findPost(postId);
        return PostDetailResponse.of(
                post,
                findNickname(post.getUserId()),
                commentRepository.countByPostId(postId)
        );
    }

    /** 게시글의 댓글 목록을 오래된 순으로 조회한다(최대 조회 개수 제한). */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId) {
        findPost(postId);

        Pageable pageable = PageRequest.of(0, MAX_COMMENT_SIZE);
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAscIdAsc(postId, pageable);
        if (comments.isEmpty()) {
            return List.of();
        }

        Map<Long, String> nicknames = findNicknames(comments.stream().map(Comment::getUserId).toList());

        return comments.stream()
                .map(comment -> CommentResponse.of(comment, nicknames.get(comment.getUserId())))
                .toList();
    }

    /** 댓글 또는 대댓글을 등록한다. */
    @Transactional
    public Long createComment(Long postId, Long userId, CommentCreateRequest request) {
        Post post = findPost(postId);

        Long parentCommentId = request.parentCommentId();
        if (parentCommentId != null) {
            Comment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new CustomException(CommonErrorCode.NOT_FOUND));
            if (!parent.getPostId().equals(postId)) {
                throw new CustomException(CommonErrorCode.INVALID_INPUT);
            }
            if (parent.getParentCommentId() != null) {
                throw new CustomException(CommonErrorCode.INVALID_INPUT);
            }
        }

        Comment comment = Comment.create(postId, userId, parentCommentId, request.content());
        Long commentId = commentRepository.save(comment).getId();

        // 원글 작성자에게 새 댓글 알림을 발송한다. 자기 글에 자기가 단 댓글은 제외.
        Long postAuthorId = post.getUserId();
        if (!postAuthorId.equals(userId)) {
            webPushSender.sendToUser(
                    postAuthorId,
                    new WebPushPayload(
                            "새 댓글",
                            "회원님의 게시글에 새 댓글이 달렸어요.",
                            "BOARD",
                            "/posts/" + postId
                    )
            );
        }

        return commentId;
    }

    private Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.NOT_FOUND));
    }

    private String findNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElse(null);
    }

    private Map<Long, String> findNicknames(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
    }

    private Map<Long, Long> findCommentCounts(List<Long> postIds) {
        return commentRepository.countGroupedByPostIds(postIds).stream()
                .collect(Collectors.toMap(CommentCount::postId, CommentCount::commentCount));
    }
}
