package com.leets.tdd.board.dto;

import com.leets.tdd.board.domain.Post;

import java.time.LocalDateTime;

public record PostDetailResponse(
        Long postId,
        String title,
        String content,
        String authorNickname,
        long commentCount,
        LocalDateTime createdAt
) {
    public static PostDetailResponse of(Post post, String authorNickname, long commentCount) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                authorNickname,
                commentCount,
                post.getCreatedAt()
        );
    }
}
