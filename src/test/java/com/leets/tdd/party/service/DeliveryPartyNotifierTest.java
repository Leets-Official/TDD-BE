package com.leets.tdd.party.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leets.tdd.global.webpush.WebPushPayload;
import com.leets.tdd.global.webpush.WebPushSender;
import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantRole;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeliveryPartyNotifierTest {

    @Mock
    private PartyParticipantRepository partyParticipantRepository;

    @Mock
    private WebPushSender webPushSender;

    @Test
    void 모집_마감_알림은_파티장을_제외한_참여자에게_보낸다() {
        DeliveryParty party = party(10L, 1L);
        PartyParticipant host = new PartyParticipant(10L, 1L, PartyParticipantRole.HOST,
                PartyParticipantStatus.JOINED, LocalDateTime.now());
        PartyParticipant member = new PartyParticipant(10L, 2L, PartyParticipantRole.MEMBER,
                PartyParticipantStatus.JOINED, LocalDateTime.now());
        when(partyParticipantRepository.findAllByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(List.of(host, member));

        DeliveryPartyNotifier notifier = new DeliveryPartyNotifier(
                partyParticipantRepository, webPushSender
        );
        notifier.notifyRecruitmentClosed(party);

        ArgumentCaptor<WebPushPayload> payloadCaptor = ArgumentCaptor.forClass(WebPushPayload.class);
        verify(webPushSender).sendToUsers(eq(List.of(2L)), payloadCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(payloadCaptor.getValue().title())
                .isEqualTo("배달팟 모집이 마감되었어요");
        org.assertj.core.api.Assertions.assertThat(payloadCaptor.getValue().category()).isEqualTo("POT");
    }

    private DeliveryParty party(Long id, Long creatorId) {
        DeliveryParty party = new DeliveryParty(
                creatorId, 1L, "치킨 같이 시켜요", "오늘 저녁 배달팟", 2, 4,
                LocalDateTime.of(2026, 7, 30, 19, 30), PartyStatus.RECRUITING, null,
                SettlementStatus.NONE, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }
}
