package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해주세요.")
        String password,

        @Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]*$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String nickname,

        @Pattern(regexp = "^(1기숙사|2기숙사|3기숙사)?$", message = "기숙사 동을 선택하세요(1기숙사).")
        String dormitory
) {
}
