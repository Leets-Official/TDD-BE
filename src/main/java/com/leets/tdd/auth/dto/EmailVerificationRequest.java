package com.leets.tdd.auth.dto;

import com.leets.tdd.auth.domain.EmailPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


public record EmailVerificationRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Pattern(regexp = "^[\\w.-]+@gachon\\.ac\\.kr$", message = "학교 이메일 형식이 아닙니다.")
        String email,

        @NotNull(message = "purpose를 입력해주세요.")
        EmailPurpose purpose
) {
}
