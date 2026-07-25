package com.leets.tdd.report.exception;

import com.leets.tdd.global.common.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.leets.tdd.report.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReportExceptionHandler {

  @ExceptionHandler(ReportException.class)
  public ResponseEntity<ApiResponse<Void>> handleReportException(ReportException exception) {
    ReportErrorCode errorCode = exception.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.fail(errorCode.getMessage()));
  }

  @ExceptionHandler(CannotAcquireLockException.class)
  public ResponseEntity<ApiResponse<Void>> handleLockFailure(CannotAcquireLockException exception) {
    return ResponseEntity.status(ReportErrorCode.REPORT_ALREADY_EXISTS.getHttpStatus())
        .body(ApiResponse.fail("신고 처리 중입니다. 잠시 후 다시 시도해주세요."));
  }
}
