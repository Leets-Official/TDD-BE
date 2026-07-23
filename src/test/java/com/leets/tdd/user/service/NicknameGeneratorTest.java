package com.leets.tdd.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NicknameGeneratorTest {

    private final NicknameGenerator nicknameGenerator = new NicknameGenerator();

    @Test
    @DisplayName("생성된 닉네임은 30자 제한(User.nickname) 이내다")
    void generate_withinLengthLimit() {
        String nickname = nicknameGenerator.generate();

        assertThat(nickname).isNotBlank();
        assertThat(nickname.length()).isLessThanOrEqualTo(30);
    }

    @Test
    @DisplayName("여러 번 생성해도 대부분 서로 다른 닉네임이 나온다(조합 수가 충분함)")
    void generate_producesVariedNicknames() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            generated.add(nicknameGenerator.generate());
        }

        assertThat(generated.size()).isGreaterThan(990);
    }
}
