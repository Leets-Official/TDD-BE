package com.leets.tdd.chat.domain;

public enum MessageType {
    USER,                     // 일반 사용자 메시지
    IMAGE,                    // 이미지 메시지
    DELIVERY_ARRIVED,         // 시스템: 배달 도착
    DELIVERY_ARRIVED_CANCEL,  // 시스템: 배달 도착 취소
    SETTLEMENT_REQUEST,       // 시스템: 정산 요청(계좌 포함)
    ORDER_COMPLETED           // 시스템: 주문 완료
}
