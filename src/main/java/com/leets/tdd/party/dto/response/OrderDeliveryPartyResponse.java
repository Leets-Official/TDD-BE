package com.leets.tdd.party.dto.response;

public record OrderDeliveryPartyResponse(
    Long partyId,
    String status,
    String settlementStatus
) {
}
