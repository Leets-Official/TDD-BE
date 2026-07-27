package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 프로필 이미지 업로드 3단계(확정) 요청. presign에서 받은 key를 그대로 보낸다. 서버는 이 key를
 * 그대로 신뢰하지 않고, 호출자 본인 몫인지 + 실제로 S3에 업로드됐는지를 다시 검증한 뒤에만
 * User.profileImageUrl(실제로는 key를 저장)에 반영한다.
 */
public record ProfileImageConfirmRequest(

        @NotBlank(message = "key를 입력해주세요.")
        String key
) {
}
