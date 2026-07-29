package com.leets.tdd.chat.service;

import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.repository.ChatMessageRepository;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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

    @InjectMocks
    private ChatService chatService;

    private static final Long PARTY_ID = 1L;

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
}
