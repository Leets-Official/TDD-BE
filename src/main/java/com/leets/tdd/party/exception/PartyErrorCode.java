package com.leets.tdd.party.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PartyErrorCode {

    PARTY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "존재하지 않는 배달팟입니다."
    );

    private final HttpStatus status;
    private final String message;

    PartyErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
