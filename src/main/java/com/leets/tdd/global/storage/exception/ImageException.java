package com.leets.tdd.global.storage.exception;

import com.leets.tdd.global.error.CustomException;

public class ImageException extends CustomException {

    public ImageException(ImageErrorCode errorCode) {
        super(errorCode);
    }
}
