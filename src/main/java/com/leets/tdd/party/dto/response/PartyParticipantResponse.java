package com.leets.tdd.party.dto.response;

public record PartyParticipantResponse(
        Long userId,
        String nickname,
        String profileImage,
        String role
) {
}
