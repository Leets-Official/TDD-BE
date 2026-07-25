package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 마이페이지 > 비밀번호 수정 요청. 로그인 상태(access token)에서 본인 확인용으로
 * 현재 비밀번호를 한 번 더 받는다. newPassword 필드명/검증 메시지는 ResetPasswordRequest와 통일한다.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해주세요.")
        String newPassword
) {
}
