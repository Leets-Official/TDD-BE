package com.leets.tdd.chat.dto;

import com.leets.tdd.chat.domain.ChatMessage;
import com.leets.tdd.chat.domain.MessageType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        MessageType messageType,
        Long senderId,
        String senderNickname,
        String content,
        String imageUrl,
        LocalDateTime createdAt
) {
    // 엔티티 → DTO 변환 (닉네임 없이 - 시스템 메시지나 단건 변환용)
    public static ChatMessageResponse from(ChatMessage message) {
        return from(message, null);
    }

    // 엔티티 → DTO 변환 (닉네임 포함 - 메시지 목록 조회에서 사용)
    public static ChatMessageResponse from(ChatMessage message, String senderNickname) {
        return new ChatMessageResponse(
                message.getId(),
                message.getMessageType(),
                message.getSenderId(),
                senderNickname,
                message.getContent(),
                message.getImageUrl(),
                message.getCreatedAt()
        );
    }
}
