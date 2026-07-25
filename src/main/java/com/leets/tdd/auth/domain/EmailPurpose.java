package com.leets.tdd.auth.domain;

/**
 * 이메일 인증코드 요청 목적.
 * purpose 값(SIGNUP / RESET_PASSWORD)
 */
public enum EmailPurpose {
    SIGNUP,
    RESET_PASSWORD
}
