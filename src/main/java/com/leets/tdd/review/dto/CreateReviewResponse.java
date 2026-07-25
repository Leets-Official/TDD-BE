package com.leets.tdd.review.dto;

import com.leets.tdd.review.domain.Review;
import java.time.LocalDateTime;
import java.util.List;

public record CreateReviewResponse(
    Long reviewId,
    Long partyId,
    Long revieweeId,
    Integer rating,
    List<Long> tagIds,
    String content,
    LocalDateTime createdAt
) {

  public static CreateReviewResponse from(Review review, List<Long> tagIds) {
    return new CreateReviewResponse(
        review.getId(),
        review.getPartyId(),
        review.getRevieweeId(),
        review.getRating(),
        tagIds,
        review.getContent(),
        review.getCreatedAt()
    );
  }
}
