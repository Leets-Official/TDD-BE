package com.leets.tdd.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * access/refresh JWT 발급 및 파싱을 담당한다.
 * jjwt 예외(ExpiredJwtException, JwtException 등)를 그대로 던지니, 호출부(Service)에서
 * 잡아서 각 도메인의 에러코드(예: UserErrorCode.TOKEN_EXPIRED/INVALID_TOKEN)로 변환해야 한다.
 * 이 클래스는 특정 도메인에 의존하지 않는 공용 인프라라 auth/user 어디서든 재사용 가능하다.
 */
@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
            @Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = Duration.ofSeconds(accessTokenValiditySeconds);
        this.refreshTokenValidity = Duration.ofSeconds(refreshTokenValiditySeconds);
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, TYPE_ACCESS, accessTokenValidity);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, TYPE_REFRESH, refreshTokenValidity);
    }

    public Duration getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    /**
     * "Bearer xxx" 형태의 Authorization 헤더 값에서 토큰만 추출한다.
     * 형식이 안 맞으면(헤더 없음/Bearer 접두사 없음) null을 반환한다.
     */
    public String resolveToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    /**
     * 토큰을 파싱해서 subject(사용자 id)를 반환한다.
     * 서명이 잘못됐거나 형식이 깨졌으면 JwtException 계열이, 만료됐으면 ExpiredJwtException이 던져진다.
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    private String createToken(Long userId, String type, Duration validity) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validity.toMillis());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
