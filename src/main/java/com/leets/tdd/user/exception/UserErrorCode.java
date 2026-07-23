package com.leets.tdd.user.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode {

    NICKNAME_DUPLICATE(HttpStatus.BAD_REQUEST, "닉네임이 중복입니다."),
    INVALID_VERIFICATION(HttpStatus.BAD_REQUEST, "인증정보가 유효하지 않습니다. 이메일 인증을 다시 해주세요."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "인증 토큰이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "이용이 제한된 계정입니다."),
    WITHDRAWAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "이용 제한 중에는 탈퇴할 수 없습니다."),
    REGISTRATION_BLOCKED(HttpStatus.BAD_REQUEST, "가입할 수 없는 이메일입니다."),
    ALREADY_REGISTERED_EMAIL(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다."),
    NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "닉네임 자동 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}
