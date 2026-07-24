package com.leets.tdd.chat.dto;

import com.leets.tdd.chat.domain.MessageType;

public record ChatMessageRequest(
        MessageType messageType,
        String content,
        String imageUrl
) {
}
