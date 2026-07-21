package com.leets.tdd.auth.dto;

import com.leets.tdd.auth.domain.EmailPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 인증코드 확인 API 요청.
 * purpose는 선택값이다. 생략하면 SIGNUP(회원가입)으로 간주하고,
 * RESET_PASSWORD(비밀번호 재설정) 흐름에서는 purpose를 명시한다.
 */
public record VerifyEmailCodeRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Pattern(regexp = "^[\\w.-]+@gachon\\.ac\\.kr$", message = "학교 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증코드를 입력해주세요.")
        @Pattern(regexp = "^[0-9]{6}$", message = "인증코드는 숫자 6자리여야 합니다.")
        String code,

        EmailPurpose purpose
) {
    public EmailPurpose purposeOrDefault() {
        return purpose != null ? purpose : EmailPurpose.SIGNUP;
    }
}
