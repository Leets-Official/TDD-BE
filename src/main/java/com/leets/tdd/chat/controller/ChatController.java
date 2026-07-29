package com.leets.tdd.chat.controller;

import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.dto.ChatMessageRequest;
import com.leets.tdd.chat.dto.ChatMessageResponse;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import com.leets.tdd.chat.service.ChatAuthValidator;
import com.leets.tdd.chat.service.ChatNotifier;
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
    private final ChatAuthValidator chatAuthValidator;
    private final ChatNotifier chatNotifier;

    @MessageMapping("/parties/{partyId}/chat")
    public void sendMessage(
            @DestinationVariable Long partyId,
            ChatMessageRequest request,
            Principal principal
    ) {
        // CONNECT 시 StompAuthChannelInterceptor가 심은 인증 주체에서 발신자 도출
        Long senderId = ((UserPrincipal) principal).userId();

        // 발신자가 이 팟의 채팅 접근 권한(방장이거나 JOINED 참여자)이 있는지 검증한다.
        chatAuthValidator.validateChatAccess(partyId, senderId);

        ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        Long chatRoomId = chatRoom.getId();

        ChatMessageResponse response = chatService.saveMessage(chatRoomId, senderId, request);
        messagingTemplate.convertAndSend("/topic/parties/" + partyId + "/chat", response);

        // 저장·브로드캐스트 후 참여자(발신자 제외)에게 새 메시지 알림 발송
        chatNotifier.notifyNewMessage(partyId, senderId);
    }
}
