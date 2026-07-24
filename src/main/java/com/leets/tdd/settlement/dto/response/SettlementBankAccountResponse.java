package com.leets.tdd.settlement.dto.response;

public record SettlementBankAccountResponse(
    String bankName,
    String accountNumber,
    String accountHolder
) {
}
