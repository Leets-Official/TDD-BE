package com.leets.tdd.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank(message = "refreshToken을 입력해주세요.")
        String refreshToken
) {
}
