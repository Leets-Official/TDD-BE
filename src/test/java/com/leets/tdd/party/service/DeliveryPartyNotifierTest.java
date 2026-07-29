package com.leets.tdd.party.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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
        org.assertj.core.api.Assertions.assertThat(payloadCaptor.getValue().url()).isEqualTo("/parties/10");
    }

    @Test
    void 참여와_퇴장_알림은_파티장에게_정확한_페이로드로_보낸다() {
        DeliveryParty party = party(10L, 1L);
        DeliveryPartyNotifier notifier = new DeliveryPartyNotifier(
                partyParticipantRepository, webPushSender
        );

        notifier.notifyParticipantJoined(party);
        notifier.notifyParticipantLeft(party);

        ArgumentCaptor<WebPushPayload> payloadCaptor = ArgumentCaptor.forClass(WebPushPayload.class);
        verify(webPushSender, times(2)).sendToUser(eq(1L), payloadCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(payloadCaptor.getAllValues())
                .extracting(WebPushPayload::title)
                .containsExactly("새로운 참여자가 생겼어요", "참여자가 나갔어요");
        org.assertj.core.api.Assertions.assertThat(payloadCaptor.getAllValues())
                .allSatisfy(payload -> {
                    org.assertj.core.api.Assertions.assertThat(payload.body()).contains("치킨 같이 시켜요");
                    org.assertj.core.api.Assertions.assertThat(payload.category()).isEqualTo("POT");
                    org.assertj.core.api.Assertions.assertThat(payload.url()).isEqualTo("/parties/10");
                });
    }

    @Test
    void 상태_변경_알림은_참여자에게_각_상태별_페이로드로_보낸다() {
        DeliveryParty party = party(10L, 1L);
        PartyParticipant member = new PartyParticipant(10L, 2L, PartyParticipantRole.MEMBER,
                PartyParticipantStatus.JOINED, LocalDateTime.now());
        when(partyParticipantRepository.findAllByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(List.of(member));
        DeliveryPartyNotifier notifier = new DeliveryPartyNotifier(
                partyParticipantRepository, webPushSender
        );

        notifier.notifyRecruitmentClosed(party);
        notifier.notifyOrderCompleted(party);
        notifier.notifyDeliveryCompleted(party);
        notifier.notifyPartyCanceled(party);

        ArgumentCaptor<WebPushPayload> payloadCaptor = ArgumentCaptor.forClass(WebPushPayload.class);
        verify(webPushSender, times(4)).sendToUsers(eq(List.of(2L)), payloadCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(payloadCaptor.getAllValues())
                .extracting(WebPushPayload::title)
                .containsExactly(
                        "배달팟 모집이 마감되었어요",
                        "주문이 완료되었어요",
                        "배달이 완료되었어요",
                        "배달팟이 취소되었어요"
                );
        org.assertj.core.api.Assertions.assertThat(payloadCaptor.getAllValues())
                .allSatisfy(payload -> {
                    org.assertj.core.api.Assertions.assertThat(payload.body()).contains("치킨 같이 시켜요");
                    org.assertj.core.api.Assertions.assertThat(payload.category()).isEqualTo("POT");
                    org.assertj.core.api.Assertions.assertThat(payload.url()).isEqualTo("/parties/10");
                });
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
