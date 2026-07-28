package com.leets.tdd.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채팅 이미지 업로드 URL 발급 요청")
public record ChatImagePresignRequest(
        @Schema(description = "업로드할 이미지의 Content-Type", example = "image/jpeg")
        @NotBlank(message = "contentType은 필수입니다.")
        String contentType
) {
}
