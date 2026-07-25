package com.leets.tdd.chat.dto;

import com.leets.tdd.chat.domain.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long chatRoomId,
        Long partyId,
        LocalDateTime createdAt
) {
    // 엔티티 → DTO 변환
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return new ChatRoomResponse(
                chatRoom.getId(),
                chatRoom.getPartyId(),
                chatRoom.getCreatedAt()
        );
    }
}
