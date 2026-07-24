package com.leets.tdd.global.auth;

/**
 * JwtAuthenticationFilter가 토큰 검증에 실패한 이유를 request attribute로 넘길 때 쓰는 타입.
 * JwtAuthenticationEntryPoint가 이 값을 보고 "만료" vs "그 외 무효" 응답 메시지를 구분한다.
 */
public enum JwtAuthErrorType {
    MISSING,
    EXPIRED,
    INVALID,
    // 서명/만료는 정상이지만 DB 조회 결과 이미 탈퇴/정지된 계정인 경우.
    BANNED
}
