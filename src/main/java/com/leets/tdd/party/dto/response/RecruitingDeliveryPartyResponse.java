package com.leets.tdd.party.dto.response;

import java.time.LocalDateTime;

public record RecruitingDeliveryPartyResponse(
        Long partyId,
        String title,
        String category,
        Integer currentParticipants,
        Integer minParticipants,
        Integer maxParticipants,
        String status,
        LocalDateTime orderExpectedAt,
        String dormitory
) {
}
