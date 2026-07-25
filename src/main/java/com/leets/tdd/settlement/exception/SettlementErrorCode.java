package com.leets.tdd.settlement.exception;

import com.leets.tdd.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode implements ErrorCode {
  PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "배달팟을 찾을 수 없습니다."),
  NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "배달팟 참여자만 이용할 수 있습니다."),
  NOT_HOST(HttpStatus.FORBIDDEN, "방장만 정산을 처리할 수 있습니다."),
  PARTY_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 배달팟에서만 정산할 수 있습니다."),
  SETTLEMENT_ALREADY_REQUESTED(HttpStatus.CONFLICT, "이미 진행 중인 정산 요청이 있습니다."),
  SETTLEMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 정산입니다."),
  SETTLEMENT_NOT_REQUESTED(HttpStatus.BAD_REQUEST, "진행 중인 정산 요청이 없습니다."),
  BANK_ACCOUNT_NOT_FOUND(HttpStatus.BAD_REQUEST, "정산 계좌를 등록해주세요."),
  BANK_ACCOUNT_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 등록된 계좌가 있습니다."),
  INVALID_PAYMENT_TARGET(HttpStatus.BAD_REQUEST, "정산 대상에 참여자가 아닌 회원이 있습니다."),
  HOST_CANNOT_BE_PAYMENT_TARGET(HttpStatus.BAD_REQUEST, "방장 본인은 정산 대상에서 제외됩니다."),
  DUPLICATE_PAYMENT_TARGET(HttpStatus.BAD_REQUEST, "정산 대상이 중복되었습니다."),
  PAYMENT_SUM_EXCEEDED(HttpStatus.BAD_REQUEST, "참여자 금액의 합이 정산 총액을 넘을 수 없습니다."),
  PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 대상 송금 정보를 찾을 수 없습니다."),
  PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 송금 완료 처리되었습니다."),
  PAYMENT_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "송금 완료 처리된 내역이 없습니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

  private final HttpStatus httpStatus;
  private final String message;
}
