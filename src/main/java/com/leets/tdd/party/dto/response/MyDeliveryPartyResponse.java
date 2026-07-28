package com.leets.tdd.party.dto.response;

import java.time.LocalDateTime;

public record MyDeliveryPartyResponse(
        Long partyId,
        String title,
        String category,
        Integer currentParticipants,
        Integer maxParticipants,
        String status,
        LocalDateTime orderExpectedAt,
        String dormitory
) {
}
