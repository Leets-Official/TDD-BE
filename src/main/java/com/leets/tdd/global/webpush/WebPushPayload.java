package com.leets.tdd.global.webpush;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 웹푸시로 보낼 알림 내용. 프론트(서비스워커)가 이 JSON을 받아 화면에 표시한다.
 * category는 프론트의 알림 설정(배달팟/채팅/게시판) 필터링에 쓰인다.
 */
public record WebPushPayload(
        String title,
        String body,
        String category,   // POT / CHAT / BOARD
        String url          // 클릭 시 이동할 경로 (선택)
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public byte[] toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("웹푸시 페이로드 직렬화 실패", e);
        }
    }
}
