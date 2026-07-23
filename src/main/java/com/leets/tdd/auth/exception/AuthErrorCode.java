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

    ALREADY_REGISTERED_EMAIL(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다."),
    VERIFICATION_REQUEST_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증코드 요청 횟수를 초과했습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),
    CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다."),
    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증코드가 만료되었습니다. 다시 요청해주세요."),
    CODE_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "이미 사용된 인증코드입니다. 다시 요청해주세요."),
    VERIFICATION_ATTEMPT_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증코드 확인 시도 횟수를 초과했습니다. 다시 요청해주세요."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다"),
    LOGIN_ATTEMPT_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도 횟수를 초과했습니다. 잠시 후에 다시 시도해주세요."),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "이용이 제한된 계정입니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다. 다시 로그인해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}
