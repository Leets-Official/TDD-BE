package com.leets.tdd.chat.controller;

import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.dto.ChatMessageRequest;
import com.leets.tdd.chat.dto.ChatMessageResponse;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import com.leets.tdd.chat.service.ChatService;
import com.leets.tdd.global.jwt.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomRepository chatRoomRepository;

    @MessageMapping("/parties/{partyId}/chat")
    public void sendMessage(
            @DestinationVariable Long partyId,
            ChatMessageRequest request,
            Principal principal
    ) {
        // CONNECT 시 StompAuthChannelInterceptor가 심은 인증 주체에서 발신자 도출
        Long senderId = ((UserPrincipal) principal).userId();

        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        Long chatRoomId = chatRoom.getId();

        ChatMessageResponse response = chatService.saveMessage(chatRoomId, senderId, request);
        messagingTemplate.convertAndSend("/topic/parties/" + partyId + "/chat", response);
    }
}
