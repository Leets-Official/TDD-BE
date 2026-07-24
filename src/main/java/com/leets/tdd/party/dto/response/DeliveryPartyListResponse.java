package com.leets.tdd.party.dto.response;

import java.time.LocalDateTime;

public record DeliveryPartyListResponse(
        Long partyId,
        String title,
        String category,
        String leaderNickname,
        Integer currentParticipants,
        Integer maxParticipants,
        String status,
        LocalDateTime orderExpectedAt
) {
}
