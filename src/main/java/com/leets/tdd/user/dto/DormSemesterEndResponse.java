package com.leets.tdd.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 기숙사 인증 학기 만료일 계산 결과. 관리자가 DB로 직접 승인 처리할 때
 * dorm_verified_until 컬럼에 넣을 값을 손으로 계산하지 않도록 돕는 용도다.
 */
public record DormSemesterEndResponse(
        @JsonProperty("dorm_verified_until") LocalDateTime dormVerifiedUntil
) {
}
