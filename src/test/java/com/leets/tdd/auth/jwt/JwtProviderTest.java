package com.leets.tdd.auth.jwt;

import com.leets.tdd.global.jwt.JwtProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-for-jwt-provider-unit-test-must-be-long-enough";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 1800, 2592000);

    @Test
    @DisplayName("access token을 발급하고 파싱하면 같은 userId가 나온다")
    void createAndParseAccessToken() {
        String token = jwtProvider.createAccessToken(42L);

        Long userId = jwtProvider.parseUserId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    @DisplayName("refresh token을 발급하고 parseRefreshUserId로 파싱하면 같은 userId가 나온다")
    void createAndParseRefreshToken() {
        String token = jwtProvider.createRefreshToken(7L);

        Long userId = jwtProvider.parseRefreshUserId(token);

        assertThat(userId).isEqualTo(7L);
    }

    @Test
    @DisplayName("refresh token을 parseUserId(access 전용)로 파싱하면 예외가 발생한다")
    void parseUserId_rejectsRefreshToken() {
        String refreshToken = jwtProvider.createRefreshToken(7L);

        assertThatThrownBy(() -> jwtProvider.parseUserId(refreshToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("access token을 parseRefreshUserId(refresh 전용)로 파싱하면 예외가 발생한다")
    void parseRefreshUserId_rejectsAccessToken() {
        String accessToken = jwtProvider.createAccessToken(42L);

        assertThatThrownBy(() -> jwtProvider.parseRefreshUserId(accessToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("유효기간이 지난 토큰을 파싱하면 ExpiredJwtException이 발생한다")
    void parseExpiredToken() throws InterruptedException {
        JwtProvider shortLivedProvider = new JwtProvider(SECRET, 0, 0);
        String token = shortLivedProvider.createAccessToken(1L);

        Thread.sleep(10);

        assertThatThrownBy(() -> shortLivedProvider.parseUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("다른 secret으로 발급된 토큰은 서명 검증에 실패한다")
    void parseTokenWithDifferentSecret() {
        String token = jwtProvider.createAccessToken(1L);
        JwtProvider otherProvider = new JwtProvider(
                "a-completely-different-secret-key-for-this-unit-test-case", 1800, 2592000);

        assertThatThrownBy(() -> otherProvider.parseUserId(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Bearer 접두사가 있는 헤더에서 토큰만 추출한다")
    void resolveToken() {
        assertThat(jwtProvider.resolveToken("Bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(jwtProvider.resolveToken(null)).isNull();
        assertThat(jwtProvider.resolveToken("abc.def.ghi")).isNull();
    }
}
