package com.leets.tdd.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 프로필 이미지 업로드 확정 응답. profileImageUrl은 공개 버킷 base URL + key로 조립한, 만료되지
 * 않는 완성된 URL이다(비공개 버킷의 Presigned GET과 달리 매번 새로 발급할 필요가 없다).
 */
public record ProfileImageUploadResponse(
        @JsonProperty("profile_image_url") String profileImageUrl
) {
}
