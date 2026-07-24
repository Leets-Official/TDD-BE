package com.leets.tdd.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 찾기(재설정) 마지막 단계 요청.
 * 별도 토큰 없이, email + 그 이메일로 RESET_PASSWORD 목적 인증에 성공한 기록(15분 이내,
 * EmailVerificationService.consumePasswordResetVerification)만으로 신원을 확인한다.
 */
public record ResetPasswordRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Pattern(regexp = "^[\\w.-]+@gachon\\.ac\\.kr$", message = "학교 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해주세요.")
        String newPassword
) {
}
