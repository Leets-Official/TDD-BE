package com.leets.tdd.global.error;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final CommonErrorCode errorCode;

    public CustomException(CommonErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
