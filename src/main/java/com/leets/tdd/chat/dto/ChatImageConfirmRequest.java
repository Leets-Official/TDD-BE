package com.leets.tdd.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채팅 이미지 업로드 확정 요청")
public record ChatImageConfirmRequest(
        @Schema(description = "발급 단계에서 받은 S3 객체 key",
                example = "chat/1/1f0a2c4e-1b3d-4f5a-8c9d-0e1f2a3b4c5d.jpg")
        @NotBlank(message = "key는 필수입니다.")
        String key
) {
}
