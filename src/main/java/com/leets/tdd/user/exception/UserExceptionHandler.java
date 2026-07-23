package com.leets.tdd.user.exception;

import com.leets.tdd.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/*
 * @Order 없이는 GlobalExceptionHandler(순서 미지정 -> Ordered.LOWEST_PRECEDENCE로 취급)의
 * catch-all(Exception.class) 핸들러가 먼저 매칭돼서 UserException이 항상 500으로 새어나간다.
 * (ExceptionHandlerExceptionResolver는 advice들을 순서대로 순회하며 "처음 매칭되는" 핸들러를 쓰고,
 *  같은 advice 안에서만 예외 타입의 상속 깊이를 따져 가장 구체적인 메서드를 고른다.)
 * global 패키지는 건드리지 않고, 여기서만 우선순위를 명시해서 해결한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.leets.tdd.user.controller")
public class UserExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserException(UserException e) {
        UserErrorCode code = e.getErrorCode();
        log.warn("UserException: {}", code.name());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.fail(code.getMessage()));
    }
}
