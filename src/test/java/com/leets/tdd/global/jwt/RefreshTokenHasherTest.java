package com.leets.tdd.global.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher refreshTokenHasher = new RefreshTokenHasher();

    @Test
    @DisplayName("같은 입력이면 항상 같은 해시를 반환한다")
    void hash_isDeterministic() {
        String token = "some-refresh-token-value";

        String first = refreshTokenHasher.hash(token);
        String second = refreshTokenHasher.hash(token);

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(token);
    }

    @Test
    @DisplayName("72바이트를 넘는 JWT 형태의 긴 문자열도 예외 없이 해시한다(BCrypt 72바이트 제한 우회)")
    void hash_handlesTokensLongerThan72Bytes() {
        String longJwtLikeToken = "a".repeat(300) + "." + "b".repeat(300) + "." + "c".repeat(100);

        assertThatCode(() -> refreshTokenHasher.hash(longJwtLikeToken)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("입력이 다르면 해시도 다르다")
    void hash_differsForDifferentInputs() {
        String hash1 = refreshTokenHasher.hash("token-a");
        String hash2 = refreshTokenHasher.hash("token-b");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
