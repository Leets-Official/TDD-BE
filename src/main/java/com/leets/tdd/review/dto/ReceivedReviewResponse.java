package com.leets.tdd.review.dto;

import com.leets.tdd.review.domain.Review;
import java.time.LocalDateTime;
import java.util.List;

public record ReceivedReviewResponse(
    Long reviewId,
    String partyTitle,
    Integer rating,
    List<String> tags,
    String content,
    LocalDateTime createdAt
) {

  public static ReceivedReviewResponse from(Review review, String partyTitle, List<String> tags) {
    return new ReceivedReviewResponse(
        review.getId(),
        partyTitle,
        review.getRating(),
        tags,
        review.getContent(),
        review.getCreatedAt()
    );
  }
}
