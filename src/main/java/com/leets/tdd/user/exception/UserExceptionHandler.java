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
 *
 * basePackages에 auth.controller도 포함한다: AuthService.resetPassword()/logout()이
 * (INVALID_VERIFICATION/USER_NOT_FOUND 재사용을 위해) UserException을 던지는데, 이게
 * AuthController에서 발생한다. user.controller로만 범위를 좁혀두면 이 advice가 AuthController에는
 * 적용되지 않아서(ControllerAdvice는 "예외 타입"이 아니라 "어느 컨트롤러냐"로 적용 여부를 가른다)
 * GlobalExceptionHandler의 catch-all(500)로 새어나간다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {"com.leets.tdd.user.controller", "com.leets.tdd.auth.controller"})
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
