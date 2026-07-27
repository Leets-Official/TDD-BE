package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 마이페이지 > 프로필 수정 요청.
 * nickname/dormitory는 필수다. profileImageUrl은 이 API로 "설정"할 수 없다 - null/빈 문자열만
 * 허용해서 프로필 사진 해제(삭제) 용도로만 쓴다. 실제 사진 설정은 /me/profile-image/presign,
 * /me/profile-image/confirm(업로드 전용 API)으로만 가능하다. 예전에는 여기서 아무 https URL이나
 * 자유 입력으로 받아줬는데, 그러면 본인이 올리지 않은 이미지(남의 S3 key, 외부 링크 등)도 그대로
 * 등록할 수 있는 구멍이 있었다 - 업로드 API는 서버가 실제로 올라간 객체를 검증(HeadObject)한
 * 뒤에만 반영하므로 그 경로로만 설정하도록 좁혔다.
 */
public record ProfileUpdateRequest(

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]*$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
        String nickname,

        @NotBlank(message = "기숙사 동을 선택해주세요.")
        @Pattern(regexp = "^(1기숙사|2기숙사|3기숙사)$", message = "기숙사 동을 선택하세요(1기숙사).")
        String dormitory,

        // null이거나 빈 문자열이어야만 통과한다(프로필 사진 해제 전용) - 값이 있으면 무조건 거부.
        @Pattern(regexp = "^$", message = "프로필 사진은 업로드 API로만 설정할 수 있습니다. 여기서는 삭제(빈 값)만 가능합니다.")
        String profileImageUrl
) {
}
