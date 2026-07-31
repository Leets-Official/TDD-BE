package com.leets.tdd.party.dto.response;

import java.math.BigDecimal;

public record PartyParticipantResponse(
        Long userId,
        String nickname,
        String profileImage,
        BigDecimal mannerTemperature,
        String role
) {
}
