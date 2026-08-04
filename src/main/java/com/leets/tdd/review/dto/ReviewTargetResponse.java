package com.leets.tdd.review.dto;

import com.leets.tdd.user.domain.User;

public record ReviewTargetResponse(
    Long userId,
    String nickname,
    String profileImageUrl,
    boolean reviewed
) {

  // User.profileImageUrl에는 S3 객체 key가 들어 있어 그대로 내려주면 프론트가 이미지를 열 수 없다.
  // base URL과 합쳐 완성한 주소를 서비스 계층에서 받아 담는다.
  public static ReviewTargetResponse from(User user, String profileImageUrl, boolean reviewed) {
    return new ReviewTargetResponse(user.getId(), user.getNickname(), profileImageUrl, reviewed);
  }
}
