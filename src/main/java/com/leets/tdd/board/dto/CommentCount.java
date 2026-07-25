package com.leets.tdd.board.dto;

// 게시글별 댓글 수를 한 번에 담아오기 위한 조회 결과
public record CommentCount(Long postId, long commentCount) {
}
