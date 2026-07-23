package com.leets.tdd.settlement.exception;

import com.leets.tdd.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.leets.tdd.settlement.controller")
public class SettlementExceptionHandler {

  @ExceptionHandler(SettlementException.class)
  public ResponseEntity<ApiResponse<Void>> handleSettlementException(SettlementException exception) {
    SettlementErrorCode errorCode = exception.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.fail(errorCode.getMessage()));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiResponse<Void>> handleOptimisticLockFailure(
      ObjectOptimisticLockingFailureException exception
  ) {
    return ResponseEntity.status(SettlementErrorCode.SETTLEMENT_ALREADY_REQUESTED.getHttpStatus())
        .body(ApiResponse.fail("정산 정보가 변경되었습니다. 다시 조회해주세요."));
  }
}
