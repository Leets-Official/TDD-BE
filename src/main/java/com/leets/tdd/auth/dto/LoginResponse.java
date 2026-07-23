package com.leets.tdd.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
