package com.leets.tdd.global.storage;

import com.leets.tdd.global.storage.exception.ImageErrorCode;
import com.leets.tdd.global.storage.exception.ImageException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ImageCategory {

    PROFILE("profiles", Visibility.PUBLIC),
    CHAT("chat", Visibility.PRIVATE),
    DORMITORY_VERIFICATION("dormitory-verifications", Visibility.PRIVATE);

    private final String prefix;
    private final Visibility visibility;

    public static ImageCategory fromKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ImageException(ImageErrorCode.INVALID_IMAGE_KEY);
        }
        return Arrays.stream(values())
                .filter(category -> key.startsWith(category.prefix + "/"))
                .findFirst()
                .orElseThrow(() -> new ImageException(ImageErrorCode.INVALID_IMAGE_KEY));
    }

    public boolean isPubliclyReadable() {
        return visibility == Visibility.PUBLIC;
    }

    public enum Visibility {
        PUBLIC,
        PRIVATE
    }
}
