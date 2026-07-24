package com.leets.tdd.settlement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SettlementPaymentRequest(
    @NotNull @Positive Long userId,
    @NotNull @Positive Integer amount
) {
}
