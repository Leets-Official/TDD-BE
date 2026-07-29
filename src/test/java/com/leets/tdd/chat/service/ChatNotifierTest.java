package com.leets.tdd.chat.service;

import com.leets.tdd.global.webpush.WebPushPayload;
import com.leets.tdd.global.webpush.WebPushSender;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatNotifierTest {

    @Mock
    private WebPushSender webPushSender;

    @Mock
    private PartyParticipantRepository partyParticipantRepository;

    @InjectMocks
    private ChatNotifier chatNotifier;

    private static final Long PARTY_ID = 1L;
    private static final Long SENDER_ID = 10L;
    private static final Long OTHER_ID = 20L;

    private PartyParticipant participant(Long userId) {
        PartyParticipant p = new PartyParticipant(PARTY_ID, userId, null, PartyParticipantStatus.JOINED, null);
        return p;
    }

    @Test
    @DisplayName("새 메시지 발생 시 발신자를 제외한 참여자에게 CHAT 알림을 발송한다")
    void notifyNewMessage_sendsToOthers() {
        when(partyParticipantRepository.findAllByPartyIdAndStatus(PARTY_ID, PartyParticipantStatus.JOINED))
                .thenReturn(List.of(participant(SENDER_ID), participant(OTHER_ID)));

        chatNotifier.notifyNewMessage(PARTY_ID, SENDER_ID);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<WebPushPayload> payloadCaptor = ArgumentCaptor.forClass(WebPushPayload.class);
        verify(webPushSender, times(1)).sendToUsers(captor.capture(), payloadCaptor.capture());

        assertThat(captor.getValue()).containsExactly(OTHER_ID);   // 발신자 제외
        assertThat(payloadCaptor.getValue().category()).isEqualTo("CHAT");
    }

    @Test
    @DisplayName("발신자 외에 참여자가 없으면 발송하지 않는다")
    void notifyNewMessage_onlySender_skips() {
        when(partyParticipantRepository.findAllByPartyIdAndStatus(PARTY_ID, PartyParticipantStatus.JOINED))
                .thenReturn(List.of(participant(SENDER_ID)));

        chatNotifier.notifyNewMessage(PARTY_ID, SENDER_ID);

        verify(webPushSender, never()).sendToUsers(any(), any());
    }
}
