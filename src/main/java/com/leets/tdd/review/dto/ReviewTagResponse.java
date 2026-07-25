package com.leets.tdd.review.dto;

import com.leets.tdd.review.domain.ReviewTag;
import com.leets.tdd.review.domain.ReviewTagCategory;

public record ReviewTagResponse(
    Long tagId,
    ReviewTagCategory category,
    String label
) {

  public static ReviewTagResponse from(ReviewTag reviewTag) {
    return new ReviewTagResponse(reviewTag.getId(), reviewTag.getCategory(), reviewTag.getLabel());
  }
}
