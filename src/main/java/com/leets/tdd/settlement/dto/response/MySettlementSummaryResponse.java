package com.leets.tdd.settlement.dto.response;

public record MySettlementSummaryResponse(
    long pendingCount,
    long pendingAmount
) {
}
