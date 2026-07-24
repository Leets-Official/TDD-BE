package com.leets.tdd.party.exception;

import com.leets.tdd.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PartyErrorCode implements ErrorCode {

    PARTY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "존재하지 않는 배달팟입니다."
    );

    private final HttpStatus httpStatus;
    private final String message;
}
