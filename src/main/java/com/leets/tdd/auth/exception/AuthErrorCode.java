package com.leets.tdd.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * auth 도메인 전용 에러코드.
 * 팀 공통 CommonErrorCode/CustomException은 건드리지 않고 auth 패키지 안에서만 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode {

    INVALID_SCHOOL_EMAIL(HttpStatus.BAD_REQUEST, "학교 이메일 형식이 아닙니다."),
    ALREADY_REGISTERED_EMAIL(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다."),
    VERIFICATION_REQUEST_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증코드 요청 횟수를 초과했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
