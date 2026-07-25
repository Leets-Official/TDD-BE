package com.leets.tdd.user.dto;

public record ProfileUpdateResponse(
        String nickname,
        String dormitory,
        String profileImageUrl
) {
}
