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

    @Transactional
    public ChatMessageResponse saveMessage(Long chatRoomId, Long senderId, ChatMessageRequest request) {
        ChatMessage message;

        if (request.messageType() == MessageType.IMAGE) {
            message = ChatMessage.createImageMessage(chatRoomId, senderId, request.imageUrl());
        } else {
            message = ChatMessage.createUserMessage(chatRoomId, senderId, request.content());
        }

        ChatMessage saved = chatMessageRepository.save(message);
        return ChatMessageResponse.from(saved);
    }
}
