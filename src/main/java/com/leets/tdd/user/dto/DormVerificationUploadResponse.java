package com.leets.tdd.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 기숙사 인증 신청(사진 업로드) 응답. 다른 user 도메인 DTO는 camelCase 그대로 내려주지만,
 * 이 API는 명세가 snake_case 필드명을 명시하고 있어 @JsonProperty로 고정한다.
 * dormVerifiedImageUrl은 DB에 저장된 S3 key가 아니라, 응답 시점에 새로 발급한 짧은 만료
 * 시간의 Presigned GET URL이다(ImageStorageService.generatePresignedGetUrl).
 */
public record DormVerificationUploadResponse(
        @JsonProperty("dorm_status") String dormStatus,
        @JsonProperty("dorm_verified_at") LocalDateTime dormVerifiedAt,
        @JsonProperty("dorm_verified_until") LocalDateTime dormVerifiedUntil,
        @JsonProperty("dorm_verified_image_url") String dormVerifiedImageUrl
) {
}
