package com.leets.tdd.settlement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CreateSettlementRequest(
    @NotNull @Positive Integer totalAmount,
    @NotEmpty List<@Valid SettlementPaymentRequest> payments
) {
}
