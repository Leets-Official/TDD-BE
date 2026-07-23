package com.leets.tdd.chat.controller;

import com.leets.tdd.chat.dto.ChatMessageRequest;
import com.leets.tdd.chat.dto.ChatMessageResponse;
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

    // 클라이언트가 /app/parties/{partyId}/chat 으로 메시지를 보내면 여기로 들어옴
    @MessageMapping("/parties/{partyId}/chat")
    public void sendMessage(
            @DestinationVariable Long partyId,
            ChatMessageRequest request
    ) {
        // TODO: 발신자 ID는 나중에 JWT에서 꺼냄 (지금은 임시로 1L)
        Long senderId = 1L;

        // TODO: partyId로 chatRoomId 찾기 (지금은 임시로 partyId 그대로)
        Long chatRoomId = partyId;

        // 메시지 저장
        ChatMessageResponse response = chatService.saveMessage(chatRoomId, senderId, request);

        // 구독자 전원에게 브로드캐스트 → /topic/parties/{partyId}/chat
        messagingTemplate.convertAndSend("/topic/parties/" + partyId + "/chat", response);
    }
}
