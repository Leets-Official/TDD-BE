package com.leets.tdd.board.controller;

import com.leets.tdd.board.dto.CommentCreateRequest;
import com.leets.tdd.board.dto.CommentResponse;
import com.leets.tdd.board.dto.PostCreateRequest;
import com.leets.tdd.board.dto.PostDetailResponse;
import com.leets.tdd.board.dto.PostListResponse;
import com.leets.tdd.board.service.PostService;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.global.jwt.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.List;

@Tag(name = "Board", description = "기숙사 게시판 관련 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "게시글 목록 조회",
            description = "게시글을 최신순으로 조회한다. size를 지정하지 않으면 20개를 반환하며, 1~100 범위로 제한된다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PostListResponse>>> getPosts(
            @RequestParam(defaultValue = "20") int size
    ) {
        List<PostListResponse> responses = postService.getPosts(size);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록 조회에 성공하였습니다.", responses));
    }

    @Operation(summary = "게시글 작성", description = "제목과 내용으로 게시글을 작성한다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createPost(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PostCreateRequest request
    ) {
        Long postId = postService.createPost(userPrincipal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("게시글 작성에 성공하였습니다.", postId));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 본문과 댓글 수를 조회한다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable Long postId
    ) {
        PostDetailResponse response = postService.getPostDetail(postId);
        return ResponseEntity.ok(ApiResponse.success("게시글 조회에 성공하였습니다.", response));
    }

    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글을 오래된 순으로 조회한다. 대댓글은 parentCommentId로 구분한다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long postId
    ) {
        List<CommentResponse> responses = postService.getComments(postId);
        return ResponseEntity.ok(ApiResponse.success("댓글 목록 조회에 성공하였습니다.", responses));
    }

    @Operation(summary = "댓글 등록", description = "댓글을 등록한다. parentCommentId를 함께 보내면 대댓글로 등록된다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<Long>> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Long commentId = postService.createComment(postId, userPrincipal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("댓글 등록에 성공하였습니다.", commentId));
    }
}
