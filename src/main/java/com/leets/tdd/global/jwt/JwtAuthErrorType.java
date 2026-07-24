package com.leets.tdd.global.jwt;

/**
 * JwtAuthenticationFilter가 토큰 검증에 실패한 이유를 request attribute로 넘길 때 쓰는 타입.
 * JwtAuthenticationEntryPoint가 이 값을 보고 "만료" vs "그 외 무효" 응답 메시지를 구분한다.
 */
public enum JwtAuthErrorType {
    MISSING,
    EXPIRED,
    INVALID
}
