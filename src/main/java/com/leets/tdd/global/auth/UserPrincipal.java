package com.leets.tdd.global.auth;

import java.security.Principal;

/**
 * JWT 검증 뒤 SecurityContext에 저장하는 현재 로그인 사용자 정보입니다.
 * JWT의 subject를 사용자 ID로 사용한다는 팀 규칙을 코드로 표현합니다.
 */
public record UserPrincipal(
    Long userId
) implements Principal {

  @Override
  public String getName() {
    return String.valueOf(userId);
  }
}
