package com.leets.tdd.global.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 업로드용 Presigned PUT URL 발급 결과")
public record PresignedUploadResponse(

        @Schema(description = "DB에 저장할 S3 객체 key", example = "profiles/7/1f0a2c4e-1b3d-4f5a-8c9d-0e1f2a3b4c5d.jpg")
        String key,

        @Schema(description = "브라우저가 파일을 직접 PUT할 URL")
        String uploadUrl,

        @Schema(description = "업로드 시 Content-Type 헤더에 그대로 넣어야 하는 값", example = "image/jpeg")
        String contentType,

        @Schema(description = "업로드 URL 유효 시간(초)", example = "300")
        long expiresInSeconds
) {
}
