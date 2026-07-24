package com.leets.tdd.settlement.dto.response;

import java.time.LocalDateTime;

public record MyOutgoingSettlementResponse(
    Long partyId,
    String partyTitle,
    String hostNickname,
    Integer amount,
    String paymentStatus,
    LocalDateTime requestedAt
) {
}
