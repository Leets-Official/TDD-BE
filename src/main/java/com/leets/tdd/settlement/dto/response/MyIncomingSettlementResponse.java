package com.leets.tdd.settlement.dto.response;

import java.time.LocalDateTime;

public record MyIncomingSettlementResponse(
    Long partyId,
    String partyTitle,
    Integer totalAmount,
    long paidCount,
    long totalCount,
    LocalDateTime requestedAt
) {
}
