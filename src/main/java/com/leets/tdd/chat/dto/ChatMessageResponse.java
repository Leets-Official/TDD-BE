package com.leets.tdd.chat.dto;

import com.leets.tdd.chat.domain.ChatMessage;
import com.leets.tdd.chat.domain.MessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        MessageType messageType,
        Long senderId,
        String content,
        String imageUrl,
        LocalDateTime createdAt
) {
    // 엔티티 → DTO 변환
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getMessageType(),
                message.getSenderId(),
                message.getContent(),
                message.getImageUrl(),
                message.getCreatedAt()
        );
    }
}
