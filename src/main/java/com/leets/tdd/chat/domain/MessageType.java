package com.leets.tdd.chat.domain;

public enum MessageType {
    USER,                 // 일반 사용자 메시지
    IMAGE,                // 이미지 메시지
    TRANSFER_REQUEST,     // 송금 요청 (계좌 카드)
    SETTLEMENT_REQUEST,   // 정산 요청
    REVIEW_PROMPT,        // 후기 유도
    DELIVERY_ARRIVED      // 배달 도착
}
