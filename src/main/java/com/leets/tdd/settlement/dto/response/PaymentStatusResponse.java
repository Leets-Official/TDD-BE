package com.leets.tdd.settlement.dto.response;

import java.time.LocalDateTime;

public record PaymentStatusResponse(
    Long partyId,
    Long userId,
    Integer amount,
    String paymentStatus,
    LocalDateTime paidAt
) {
}
