package com.leets.tdd.auth.controller;

import com.leets.tdd.auth.dto.EmailVerificationRequest;
import com.leets.tdd.auth.service.EmailVerificationService;
import com.leets.tdd.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 / 인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "학교 이메일 인증코드 발송",
            description = "회원가입(SIGNUP) 또는 비밀번호 재설정(RESET_PASSWORD)을 위한 6자리 인증코드를 이메일로 발송한다. "
                    + "1시간에 최대 5회까지 요청 가능."
    )
    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        emailVerificationService.sendVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("인증코드가 발송되었습니다."));
    }
}
