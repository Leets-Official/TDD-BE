package com.leets.tdd.global.storage;

import com.leets.tdd.global.storage.exception.ImageErrorCode;
import com.leets.tdd.global.storage.exception.ImageException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum ImageContentType {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String extension;

    public static Optional<ImageContentType> find(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return Optional.empty();
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.mimeType.equals(normalized))
                .findFirst();
    }

    public static ImageContentType from(String mimeType) {
        return find(mimeType)
                .orElseThrow(() -> new ImageException(ImageErrorCode.UNSUPPORTED_CONTENT_TYPE));
    }
}
