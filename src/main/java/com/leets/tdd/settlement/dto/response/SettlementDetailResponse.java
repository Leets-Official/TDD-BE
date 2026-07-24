package com.leets.tdd.settlement.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record SettlementDetailResponse(
    Long partyId,
    String settlementStatus,
    Integer totalAmount,
    Integer hostAmount,
    LocalDateTime requestedAt,
    SettlementRequesterResponse requester,
    SettlementBankAccountResponse bankAccount,
    List<SettlementPaymentResponse> payments
) {
}
