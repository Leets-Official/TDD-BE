package com.leets.tdd.settlement.dto.response;

/**
 * 계좌 등록 응답. accountNumber는 마스킹된 값이다(SettlementBankAccountResponse는 정산 처리를 위해
 * 원본 번호를 그대로 노출하지만, 등록 화면 응답은 마스킹해서 돌려준다).
 */
public record BankAccountResponse(
    String bankName,
    String accountNumber,
    String accountHolder
) {
}
