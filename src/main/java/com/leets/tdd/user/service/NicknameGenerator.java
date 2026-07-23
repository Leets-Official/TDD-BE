package com.leets.tdd.user.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 닉네임을 생략한 회원가입 요청에 자동 배정할 닉네임을 생성한다.
 * "한글 단어 + 영문/숫자 랜덤 접미사" 조합. 중복 여부 확인/재시도는 호출부(UserService)에서 한다.
 */
@Component
public class NicknameGenerator {

    private static final String[] NOUNS =
            {"강아지", "감자", "고양이", "다람쥐", "너구리", "펭귄", "부엉이", "여우", "토끼", "고슴도치",
                    "너울", "라이언", "판다", "수달", "고래", "왈라비", "코알라", "카피바라", "두더지", "말미잘"};
    private static final String ALPHANUMERIC =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        return noun + randomSuffix();
    }

    private String randomSuffix() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return suffix.toString();
    }
}
