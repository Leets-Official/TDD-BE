package com.leets.tdd.board.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        String content,

        // 대댓글이면 부모 댓글 ID, 일반 댓글이면 null
        Long parentCommentId
) {
}
