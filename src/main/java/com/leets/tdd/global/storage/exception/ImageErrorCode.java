package com.leets.tdd.global.storage.exception;

import com.leets.tdd.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다. JPEG, PNG, WEBP만 업로드할 수 있습니다."),
    INVALID_IMAGE_KEY(HttpStatus.BAD_REQUEST, "올바르지 않은 이미지 경로입니다."),
    IMAGE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, "업로드된 이미지를 찾을 수 없습니다."),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 크기가 허용 범위를 초과했습니다."),
    IMAGE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장소 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
