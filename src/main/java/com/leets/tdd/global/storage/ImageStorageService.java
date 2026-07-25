package com.leets.tdd.global.storage;

import com.leets.tdd.global.storage.dto.PresignedUploadResponse;

public interface ImageStorageService {

    PresignedUploadResponse issueUploadUrl(ImageCategory category, long ownerId, String contentType);

    void confirmUpload(String key);

    String resolveViewUrl(String key);

    void delete(String key);
}
