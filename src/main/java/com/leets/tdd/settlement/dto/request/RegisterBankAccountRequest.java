package com.leets.tdd.settlement.dto.request;

import com.leets.tdd.settlement.dto.validation.ValidBank;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 마이페이지 > 계좌 등록 요청.
 * 계좌번호는 은행마다 자릿수/선행 0 여부가 달라 int/long이 아닌 문자열로 받는다
 * (entity BankAccount.accountNumber도 VARCHAR(30)).
 * A안(포맷 검증만) 결정: 실명계좌 조회 API 없이, 은행명은 Bank enum 목록에 있는지만,
 * 계좌번호는 하이픈 없이 은행 공통 자릿수 범위(10~14자리) 안에 있는지만 검증한다.
 * 은행별 정확한 자릿수는 검증하지 않는다(같은 은행도 계좌 종류마다 달라 유지보수 부담이 큼).
 */
public record RegisterBankAccountRequest(

    @NotBlank(message = "은행을 선택해주세요.")
    @ValidBank
    String bankName,

    @NotBlank(message = "계좌번호를 입력해주세요.")
    @Pattern(regexp = "^[0-9]{10,14}$", message = "계좌번호는 하이픈(-) 없이 숫자 10~14자리로 입력해주세요.")
    String accountNumber,

    @NotBlank(message = "예금주명을 입력해주세요.")
    @Size(max = 30, message = "예금주명은 30자 이하로 입력해주세요.")
    String accountHolder
) {
}
