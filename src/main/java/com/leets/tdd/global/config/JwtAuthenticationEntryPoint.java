package com.leets.tdd.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leets.tdd.global.auth.JwtAuthErrorType;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.user.exception.UserErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;


public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        Object errorType = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);

        UserErrorCode errorCode;
        if (JwtAuthErrorType.MISSING.equals(errorType)) {
            errorCode = UserErrorCode.TOKEN_MISSING;
        } else if (JwtAuthErrorType.EXPIRED.equals(errorType)) {
            errorCode = UserErrorCode.TOKEN_EXPIRED;
        } else {
            errorCode = UserErrorCode.INVALID_TOKEN;
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail(errorCode.getMessage()))
        );
    }
}
