package com.leets.tdd.party.dto.response;

public record DeliveryPartySearchItemResponse(
        Long partyId,
        String title,
        String category,
        Integer currentParticipants,
        Integer maxParticipants,
        String status
) {
}
