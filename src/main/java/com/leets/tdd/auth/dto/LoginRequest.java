package com.leets.tdd.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Pattern(regexp = "^[\\w.-]+@gachon\\.ac\\.kr$", message = "학교 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
