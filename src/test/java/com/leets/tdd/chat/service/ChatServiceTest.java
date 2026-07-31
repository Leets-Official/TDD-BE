package com.leets.tdd.chat.service;

import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.repository.ChatMessageRepository;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import com.leets.tdd.chat.domain.MessageType;
import com.leets.tdd.chat.dto.ChatMessageRequest;
import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private DeliveryPartyRepository deliveryPartyRepository;

    @Mock
    private PartyParticipantRepository partyParticipantRepository;

    @InjectMocks
    private ChatService chatService;

    private static final Long PARTY_ID = 1L;
    private static final Long CREATOR_ID = 10L;

    @Test
    @DisplayName("취소된 팟에서는 방장의 메시지 저장이 거부된다")
    void saveMessage_canceledParty_denied() {
        when(deliveryPartyRepository.findWithLockById(PARTY_ID))
                .thenReturn(Optional.of(partyWithStatus(PartyStatus.CANCELED)));

        assertThatThrownBy(() -> chatService.saveMessage(
                PARTY_ID,
                1L,
                CREATOR_ID,
                new ChatMessageRequest(MessageType.USER, "안녕하세요", null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료된 배달팟에서는 채팅을 보낼 수 없습니다.");

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("배달 완료된 팟에서는 참여자의 메시지 저장이 거부된다")
    void saveMessage_completedParty_denied() {
        when(deliveryPartyRepository.findWithLockById(PARTY_ID))
                .thenReturn(Optional.of(partyWithStatus(PartyStatus.COMPLETED)));

        assertThatThrownBy(() -> chatService.saveMessage(
                PARTY_ID,
                1L,
                20L,
                new ChatMessageRequest(MessageType.USER, "안녕하세요", null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료된 배달팟에서는 채팅을 보낼 수 없습니다.");

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("해당 팟의 채팅방이 없으면 새로 생성한다")
    void createChatRoom_notExists_creates() {
        when(chatRoomRepository.findByPartyId(PARTY_ID)).thenReturn(Optional.empty());

        chatService.createChatRoom(PARTY_ID);

        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("해당 팟의 채팅방이 이미 있으면 새로 생성하지 않는다")
    void createChatRoom_alreadyExists_skips() {
        when(chatRoomRepository.findByPartyId(PARTY_ID))
                .thenReturn(Optional.of(new ChatRoom(PARTY_ID)));

        chatService.createChatRoom(PARTY_ID);

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    private DeliveryParty partyWithStatus(PartyStatus status) {
        try {
            var constructor = DeliveryParty.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            DeliveryParty party = constructor.newInstance();
            ReflectionTestUtils.setField(party, "creatorId", CREATOR_ID);
            ReflectionTestUtils.setField(party, "status", status);
            return party;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("테스트용 DeliveryParty 생성 실패", exception);
        }
    }
}
