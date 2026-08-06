package com.leets.tdd.chat.service;

import com.leets.tdd.chat.domain.ChatMessage;
import com.leets.tdd.chat.domain.MessageType;
import com.leets.tdd.chat.dto.ChatMessageResponse;
import com.leets.tdd.global.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ChatMessage 엔티티를 응답 DTO로 변환한다.
 * IMAGE 타입 메시지의 imageUrl 필드에는 S3 key가 저장되어 있으므로 (S3 이미지 업로드 가이드 6-1 참고),
 * 응답으로 내려줄 때는 ImageStorageService.resolveViewUrl로 실제 접근 가능한 URL로 변환해서 담는다.
 * 채팅 이미지는 비공개 버킷이라 짧은 만료의 Presigned GET URL이 반환되며, 조회 시점마다 새로 발급된다.
 */
@Component
@RequiredArgsConstructor
public class ChatMessageResponseMapper {

    private final ImageStorageService imageStorageService;

    // 시스템 메시지 등 발신자 닉네임이 없는 경우
    public ChatMessageResponse toResponse(ChatMessage message) {
        return toResponse(message, null);
    }

    public ChatMessageResponse toResponse(ChatMessage message, String senderNickname) {
        return new ChatMessageResponse(
                message.getId(),
                message.getMessageType(),
                message.getSenderId(),
                senderNickname,
                message.getContent(),
                resolveImageUrl(message),
                message.getCreatedAt()
        );
    }

    private String resolveImageUrl(ChatMessage message) {
        if (message.getMessageType() != MessageType.IMAGE) {
            return null;
        }
        String key = message.getImageUrl();
        if (key == null || key.isBlank()) {
            return null;
        }
        return imageStorageService.resolveViewUrl(key);
    }
}
