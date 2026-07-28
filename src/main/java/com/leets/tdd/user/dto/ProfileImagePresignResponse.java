package com.leets.tdd.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 프로필 이미지 업로드 1단계(발급) 응답. key는 3단계(확정) 요청에 그대로 다시 실어 보내야 한다.
 * uploadUrl로는 반드시 presign 요청에 보낸 것과 같은 Content-Type 헤더를 실어 PUT해야 한다.
 */
public record ProfileImagePresignResponse(
        @JsonProperty("key") String key,
        @JsonProperty("upload_url") String uploadUrl
) {
}
