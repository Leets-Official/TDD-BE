package com.leets.tdd.auth.controller;

import com.leets.tdd.auth.dto.EmailVerificationRequest;
import com.leets.tdd.auth.dto.LoginRequest;
import com.leets.tdd.auth.dto.LoginResponse;
import com.leets.tdd.auth.dto.RefreshTokenRequest;
import com.leets.tdd.auth.dto.VerifyEmailCodeRequest;
import com.leets.tdd.auth.service.AuthService;
import com.leets.tdd.auth.service.EmailVerificationService;
import com.leets.tdd.global.auth.UserPrincipal;
import com.leets.tdd.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final AuthService authService;

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

    @Operation(
            summary = "로그인",
            description = "이메일/비밀번호로 로그인해서 access/refresh 토큰을 발급받는다. "
                    + "5분 내 3회 실패하면 15분간 로그인이 제한된다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공하였습니다.", response));
    }

    @Operation(
            summary = "토큰 재발급",
            description = "로그인 때 발급받은 refresh token으로 access/refresh 토큰을 재발급한다. "
                    + "재발급마다 refresh token도 새로 교체(rotate)되어 이전 refresh token은 더 이상 쓸 수 없다."
    )
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResponse>> reissueToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        LoginResponse response = authService.reissueToken(request);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }

    @Operation(
            summary = "로그아웃",
            description = "저장된 refresh token을 무효화한다. Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        authService.logout(userPrincipal.userId());
        return ResponseEntity.ok(ApiResponse.success("로그아웃에 성공하였습니다."));
    }
}
