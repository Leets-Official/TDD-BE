package com.leets.tdd.global.s3;

import java.util.Arrays;
import java.util.Optional;

/**
 * 업로드를 허용하는 이미지 MIME 타입. S3 key의 확장자는 클라이언트가 보낸 원본 파일명이 아니라
 * 항상 여기서 검증한 Content-Type을 기준으로 정한다(파일명 위조로 확장자를 속이는 것을 방지).
 */
public enum ImageContentType {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String extension;

    ImageContentType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageContentType> from(String mimeType) {
        return Arrays.stream(values())
                .filter(type -> type.mimeType.equalsIgnoreCase(mimeType))
                .findFirst();
    }
}
