package com.leets.tdd.chat.service;

import com.leets.tdd.chat.domain.ChatMessage;
import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.domain.MessageType;
import com.leets.tdd.chat.dto.ChatMessageRequest;
import com.leets.tdd.chat.dto.ChatMessageResponse;
import com.leets.tdd.chat.dto.ChatRoomResponse;
import com.leets.tdd.chat.repository.ChatMessageRepository;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    // 한 번에 조회 가능한 메시지 개수 범위
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final DeliveryPartyRepository deliveryPartyRepository;
    private final PartyParticipantRepository partyParticipantRepository;

    /**
     * 배달팟에 대응하는 채팅방을 생성한다.
     * 배달팟이 MATCHED로 전환되는 시점에 파티 도메인에서 이 메서드를 호출한다.
     * (채팅방은 배달팟에 1:1로 종속된다 - V6 마이그레이션 주석 참고)

     * 이미 해당 팟의 채팅방이 있으면 새로 만들지 않고 그대로 둔다. 매칭 처리가
     * 재시도되거나 중복 호출돼도 채팅방이 하나만 유지되도록 하기 위함이다.
     */
    @Transactional
    public void createChatRoom(Long partyId) {
        if (chatRoomRepository.findByPartyId(partyId).isPresent()) {
            return;
        }
        chatRoomRepository.save(new ChatRoom(partyId));
    }

    // 사용자가 보낸 메시지 저장. 시스템 메시지는 이 경로로 만들 수 없다.
    @Transactional
    public ChatMessageResponse saveMessage(
            Long partyId,
            Long chatRoomId,
            Long senderId,
            ChatMessageRequest request
    ) {
        validateMessageSendAccess(partyId, senderId);

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

    /**
     * 메시지 저장과 같은 트랜잭션에서 팟 row를 잠근 뒤 상태와 참여 권한을 확인한다.
     * 팟 종료 처리도 같은 row lock을 사용하므로, 종료 상태 전환 뒤에 메시지가 저장되는 경합을 막는다.
     */
    private void validateMessageSendAccess(Long partyId, Long userId) {
        DeliveryParty party = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("배달팟을 찾을 수 없습니다."));

        if (party.getStatus() == PartyStatus.CANCELED || party.getStatus() == PartyStatus.COMPLETED) {
            throw new IllegalArgumentException("종료된 배달팟에서는 채팅을 보낼 수 없습니다.");
        }
        if (party.getCreatorId().equals(userId)) {
            return;
        }
        if (!partyParticipantRepository
                .existsByPartyIdAndUserIdAndStatus(partyId, userId, PartyParticipantStatus.JOINED)) {
            throw new IllegalArgumentException("해당 팟의 참여자만 채팅에 접근할 수 있습니다.");
        }
    }

    // 배달팟에 속한 채팅방 정보 조회
    @Transactional(readOnly = true)
    public ChatRoomResponse getChatRoom(Long partyId) {
        return ChatRoomResponse.from(findRoomByPartyId(partyId));
    }

    // 채팅방의 최신 메시지 N개 조회
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getRecentMessages(Long partyId, int size) {
        ChatRoom chatRoom = findRoomByPartyId(partyId);
        // 0 이하나 과도하게 큰 값이 들어와도 안전하도록 조회 개수를 범위 안으로 제한한다
        int boundedSize = Math.min(Math.max(size, MIN_PAGE_SIZE), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(0, boundedSize);
        List<ChatMessage> messages =
                chatMessageRepository.findByChatRoomIdOrderByCreatedAtDescIdDesc(chatRoom.getId(), pageable);
        // DB에서는 최신순으로 가져오지만, 화면에는 오래된 메시지부터 보여야 하므로 순서를 뒤집는다
        return messages.reversed().stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    private ChatRoom findRoomByPartyId(Long partyId) {
        return chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
    }
}
