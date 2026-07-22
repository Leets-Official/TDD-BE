package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 계정등록(회원가입 완료) 요청.
 * signup_token 없이, email + email_verification_codes의 verified_at(15분 이내)로 신원을 확인한다.
 * nickname을 생략하면 자동 배정하고, dormitory는 생략 가능하다.
 */
public record ProfileRegistrationRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Pattern(regexp = "^[\\w.-]+@gachon\\.ac\\.kr$", message = "학교 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        String nickname,

        String dormitory
) {
}
