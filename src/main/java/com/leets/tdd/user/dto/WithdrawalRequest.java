package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 계정탈퇴 요청. 이미 access token으로 신원이 확인된 상태에서, 실수/탈취로 인한
 * 탈퇴를 막기 위해 비밀번호 재입력을 한 번 더 요구한다.
 */
public record WithdrawalRequest(

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
