package com.leets.tdd.review.exception;

import com.leets.tdd.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.leets.tdd.review.controller")
public class ReviewExceptionHandler {

  @ExceptionHandler(ReviewException.class)
  public ResponseEntity<ApiResponse<Void>> handleReviewException(ReviewException exception) {
    ReviewErrorCode errorCode = exception.getErrorCode();
    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(ApiResponse.fail(errorCode.getMessage()));
  }
}
