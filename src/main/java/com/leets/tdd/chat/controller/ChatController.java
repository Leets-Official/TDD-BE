package com.leets.tdd.chat.controller;

import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.dto.ChatMessageRequest;
import com.leets.tdd.chat.dto.ChatMessageResponse;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import com.leets.tdd.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomRepository chatRoomRepository;

    @MessageMapping("/parties/{partyId}/chat")
    public void sendMessage(
            @DestinationVariable Long partyId,
            ChatMessageRequest request
    ) {
        // TODO: 발신자 ID는 인증 연동 후 Principal에서 도출 (지금은 임시로 1L)
        Long senderId = 1L;

        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        Long chatRoomId = chatRoom.getId();

        ChatMessageResponse response = chatService.saveMessage(chatRoomId, senderId, request);

        messagingTemplate.convertAndSend("/topic/parties/" + partyId + "/chat", response);
    }
}
