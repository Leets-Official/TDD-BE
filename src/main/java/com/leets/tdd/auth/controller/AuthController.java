package com.leets.tdd.auth.controller;

import com.leets.tdd.auth.dto.EmailVerificationRequest;
import com.leets.tdd.auth.dto.VerifyEmailCodeRequest;
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
                    + "5분 내 최대 3회까지 요청 가능하며, 초과 시 429를 반환한다."
    )
    @PostMapping("/email/send-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        emailVerificationService.sendVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("인증코드가 발송되었습니다."));
    }

    @Operation(
            summary = "이메일 인증코드 확인",
            description = "발송된 6자리 인증코드가 일치하는지, 만료되지 않았는지 확인한다. "
                    + "purpose를 생략하면 SIGNUP(회원가입)으로 간주하고, "
                    + "RESET_PASSWORD(비밀번호 재설정)인 경우 purpose를 명시한다."
    )
    @PostMapping("/email/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
            @Valid @RequestBody VerifyEmailCodeRequest request
    ) {
        emailVerificationService.verifyCode(request);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증에 성공하였습니다."));
    }
}
