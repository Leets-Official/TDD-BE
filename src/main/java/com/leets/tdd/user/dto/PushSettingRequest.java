package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 마이페이지 > 알림 설정 요청. MVP는 전체 알림 on/off 통합 토글 1개만 받는다(카테고리 구분 없음).
 * Boolean(래퍼 타입)으로 받아서 필드 생략 시에도 @NotNull로 걸러지도록 한다
 * (primitive boolean이면 생략 시 false로 조용히 들어와서 누락을 구분할 수 없음).
 */
public record PushSettingRequest(

        @NotNull(message = "알림 설정 값을 입력해주세요.")
        Boolean pushEnabled
) {
}
