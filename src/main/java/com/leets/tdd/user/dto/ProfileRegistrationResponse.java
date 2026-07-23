package com.leets.tdd.user.dto;

public record ProfileRegistrationResponse(
        String nickname,
        String dormitory,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
