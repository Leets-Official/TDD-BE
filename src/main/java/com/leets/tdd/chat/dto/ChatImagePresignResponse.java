package com.leets.tdd.chat.dto;

import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 이미지 업로드 URL 발급 결과")
public record ChatImagePresignResponse(
        @Schema(description = "확정(confirm) 및 IMAGE 메시지 발행 시 사용할 S3 객체 key",
                example = "chat/1/1f0a2c4e-1b3d-4f5a-8c9d-0e1f2a3b4c5d.jpg")
        String key,

        @Schema(description = "브라우저가 이미지를 직접 PUT할 URL")
        String uploadUrl,

        @Schema(description = "업로드 시 Content-Type 헤더에 그대로 넣어야 하는 값", example = "image/jpeg")
        String contentType
) {
    public static ChatImagePresignResponse from(PresignedUploadResponse presigned) {
        return new ChatImagePresignResponse(
                presigned.key(),
                presigned.uploadUrl(),
                presigned.contentType()
        );
    }
}
