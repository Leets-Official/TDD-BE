package com.leets.tdd.party.dto.response;

public record LeaveDeliveryPartyResponse(
        Long partyId,
        long currentParticipants,
        Integer maxParticipants
) {
}
