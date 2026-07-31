package com.leets.tdd.chat.service;

import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.repository.ChatMessageRepository;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import com.leets.tdd.chat.domain.MessageType;
import com.leets.tdd.chat.dto.ChatMessageRequest;
import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
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
import static org.mockito.Mockito.doThrow;
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
    private ChatAuthValidator chatAuthValidator;

    @InjectMocks
    private ChatService chatService;

    private static final Long PARTY_ID = 1L;
    private static final Long CREATOR_ID = 10L;

    @Test
    @DisplayName("채팅 접근 권한 검증에서 막히면 메시지를 저장하지 않는다")
    void saveMessage_accessDenied_notSaved() {
        DeliveryParty party = partyWithStatus(PartyStatus.CANCELED);
        when(deliveryPartyRepository.findWithLockById(PARTY_ID)).thenReturn(Optional.of(party));
        doThrow(new IllegalArgumentException("종료된 배달팟에서는 채팅을 이용할 수 없습니다."))
                .when(chatAuthValidator).validateChatAccess(party, CREATOR_ID);

        assertThatThrownBy(() -> chatService.saveMessage(
                PARTY_ID,
                1L,
                CREATOR_ID,
                new ChatMessageRequest(MessageType.USER, "안녕하세요", null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료된 배달팟에서는 채팅을 이용할 수 없습니다.");

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅 접근 권한 검증을 통과하면 메시지를 저장한다")
    void saveMessage_accessGranted_saved() {
        DeliveryParty party = partyWithStatus(PartyStatus.CLOSED);
        when(deliveryPartyRepository.findWithLockById(PARTY_ID)).thenReturn(Optional.of(party));
        // chatAuthValidator는 기본 mock이라 아무것도 던지지 않음 = 통과
        when(chatMessageRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        chatService.saveMessage(
                PARTY_ID,
                1L,
                CREATOR_ID,
                new ChatMessageRequest(MessageType.USER, "안녕하세요", null)
        );

        verify(chatAuthValidator).validateChatAccess(party, CREATOR_ID);
        verify(chatMessageRepository, times(1)).save(any());
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
