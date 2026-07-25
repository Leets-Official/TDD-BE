package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 마이페이지 > 프로필 수정 요청.
 * nickname/dormitory는 필수, profileImageUrl은 비워서 보내면 프로필 사진을 해제한다.
 */
public record ProfileUpdateRequest(

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]*$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String nickname,

        @NotBlank(message = "기숙사 동을 선택해주세요.")
        @Pattern(regexp = "^(1기숙사|2기숙사|3기숙사)$", message = "기숙사 동을 선택하세요(1기숙사).")
        String dormitory,

        // null/빈 값이면 검증을 통과시키고(프로필 사진 해제) 값이 있을 때만 형식/길이를 검사한다.
        @Size(max = 500, message = "이미지 URL은 500자 이하로 입력해주세요.")
        @Pattern(regexp = "^$|^https?://.+$", message = "이미지 URL 형식이 올바르지 않습니다.")
        String profileImageUrl
) {
}
