package com.leets.tdd.party.exception;

import com.leets.tdd.global.error.CustomException;

public class PartyException extends CustomException {

    public PartyException(PartyErrorCode errorCode) {
        super(errorCode);
    }
}
