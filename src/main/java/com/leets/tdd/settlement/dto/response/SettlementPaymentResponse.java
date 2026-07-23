package com.leets.tdd.settlement.dto.response;

import java.time.LocalDateTime;

public record SettlementPaymentResponse(
    Long userId,
    String nickname,
    Integer amount,
    String paymentStatus,
    LocalDateTime paidAt
) {
}
