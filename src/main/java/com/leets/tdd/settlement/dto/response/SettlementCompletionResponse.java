package com.leets.tdd.settlement.dto.response;

public record SettlementCompletionResponse(
    Long partyId,
    String settlementStatus,
    long unpaidCount
) {
}
