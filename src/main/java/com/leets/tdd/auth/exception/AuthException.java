package com.leets.tdd.auth.exception;

import lombok.Getter;

/**
 * auth 도메인 전용 예외.
 * 팀 공통 CustomException을 상속/수정하지 않고 별도로 둔다.
 */
@Getter
public class AuthException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
