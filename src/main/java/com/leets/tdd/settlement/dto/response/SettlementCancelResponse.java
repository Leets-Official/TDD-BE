package com.leets.tdd.settlement.dto.response;

public record SettlementCancelResponse(
    Long partyId,
    String settlementStatus,
    long paidCount
) {
}
