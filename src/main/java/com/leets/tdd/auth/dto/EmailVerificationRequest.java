package com.leets.tdd.auth.dto;

import com.leets.tdd.auth.domain.EmailPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record EmailVerificationRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        String email,

        @NotNull(message = "purpose를 입력해주세요.")
        EmailPurpose purpose
) {
}
