package com.leets.tdd.settlement.exception;

import com.leets.tdd.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
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
}
