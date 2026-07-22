package com.leets.tdd.review.dto;

import com.leets.tdd.user.domain.User;

public record ReviewTargetResponse(
    Long userId,
    String nickname,
    String profileImageUrl,
    boolean reviewed
) {

  public static ReviewTargetResponse from(User user, boolean reviewed) {
    return new ReviewTargetResponse(user.getId(), user.getNickname(), user.getProfileImageUrl(), reviewed);
  }
}
