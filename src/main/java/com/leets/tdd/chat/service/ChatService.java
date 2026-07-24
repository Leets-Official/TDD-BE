package com.leets.tdd.chat.service;

import com.leets.tdd.chat.domain.ChatMessage;
import com.leets.tdd.chat.domain.MessageType;
import com.leets.tdd.chat.dto.ChatMessageRequest;
import com.leets.tdd.chat.dto.ChatMessageResponse;
import com.leets.tdd.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;

    // 사용자가 보낸 메시지 저장. 시스템 메시지는 이 경로로 만들 수 없다.
    @Transactional
    public ChatMessageResponse saveMessage(Long chatRoomId, Long senderId, ChatMessageRequest request) {
        ChatMessage message;

        if (request.messageType() == MessageType.IMAGE) {
            if (request.imageUrl() == null || request.imageUrl().isBlank()) {
                throw new IllegalArgumentException("이미지 메시지에는 이미지 URL이 필요합니다.");
            }
            message = ChatMessage.createImageMessage(chatRoomId, senderId, request.imageUrl());

        } else if (request.messageType() == MessageType.USER) {
            if (request.content() == null || request.content().isBlank()) {
                throw new IllegalArgumentException("메시지 내용이 비어 있습니다.");
            }
            message = ChatMessage.createUserMessage(chatRoomId, senderId, request.content());

        } else {
            throw new IllegalArgumentException("허용되지 않은 메시지 타입입니다.");
        }

        ChatMessage saved = chatMessageRepository.save(message);
        return ChatMessageResponse.from(saved);
    }
}
