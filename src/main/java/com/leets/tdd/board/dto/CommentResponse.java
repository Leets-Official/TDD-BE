package com.leets.tdd.board.dto;

import com.leets.tdd.board.domain.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long parentCommentId,
        String content,
        String authorNickname,
        LocalDateTime createdAt
) {
    public static CommentResponse of(Comment comment, String authorNickname) {
        return new CommentResponse(
                comment.getId(),
                comment.getParentCommentId(),
                comment.getContent(),
                authorNickname,
                comment.getCreatedAt()
        );
    }
}
