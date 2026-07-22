package com.leets.tdd.auth.exception;

import com.leets.tdd.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * auth 패키지(컨트롤러)에만 적용되는 예외 처리.
 * 팀 공통 GlobalExceptionHandler는 그대로 두고, auth 전용 예외만 여기서 처리한다.
 * @Order를 안 주면 GlobalExceptionHandler(순서 미지정 -> 가장 낮은 우선순위로 취급)의
 * catch-all이 먼저 매칭돼서 AuthException이 500으로 새어나갈 수 있어 명시적으로 우선순위를 준다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.leets.tdd.auth.controller")
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
        AuthErrorCode code = e.getErrorCode();
        log.warn("AuthException: {}", code.name());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.fail(code.getMessage()));
    }
}
