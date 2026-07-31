package com.leets.tdd.party.dto.response;

public record JoinDeliveryPartyResponse(
        Long partyId,
        long currentParticipants,
        Integer maxParticipants
) {
}
