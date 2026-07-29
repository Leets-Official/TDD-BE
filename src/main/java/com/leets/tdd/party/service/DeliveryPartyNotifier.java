package com.leets.tdd.party.service;

import com.leets.tdd.global.webpush.WebPushPayload;
import com.leets.tdd.global.webpush.WebPushSender;
import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 배달팟 상태 변경에 따른 웹푸시 알림을 담당한다. */
@Component
@RequiredArgsConstructor
public class DeliveryPartyNotifier {

    private static final String PARTY_CATEGORY = "POT";
    private static final String PARTY_URL_FORMAT = "/parties/%d";

    private final PartyParticipantRepository partyParticipantRepository;
    private final WebPushSender webPushSender;

    public void notifyParticipantJoined(DeliveryParty party) {
        webPushSender.sendToUser(party.getCreatorId(), payload(
                party, "새로운 참여자가 생겼어요", "'%s' 배달팟에 새로운 참여자가 들어왔어요."
        ));
    }

    public void notifyParticipantLeft(DeliveryParty party) {
        webPushSender.sendToUser(party.getCreatorId(), payload(
                party, "참여자가 나갔어요", "'%s' 배달팟에서 참여자가 나갔어요."
        ));
    }

    public void notifyRecruitmentClosed(DeliveryParty party) {
        notifyParticipants(party, "배달팟 모집이 마감되었어요", "'%s' 배달팟 모집이 마감되었어요.");
    }

    public void notifyOrderCompleted(DeliveryParty party) {
        notifyParticipants(party, "주문이 완료되었어요", "'%s' 주문이 완료되었어요.");
    }

    public void notifyDeliveryCompleted(DeliveryParty party) {
        notifyParticipants(party, "배달이 완료되었어요", "'%s' 배달이 완료되었어요.");
    }

    public void notifyPartyCanceled(DeliveryParty party) {
        notifyParticipants(party, "배달팟이 취소되었어요", "'%s' 배달팟이 취소되었어요.");
    }

    private void notifyParticipants(DeliveryParty party, String title, String bodyFormat) {
        List<Long> participantUserIds = partyParticipantRepository
                .findAllByPartyIdAndStatus(party.getId(), PartyParticipantStatus.JOINED)
                .stream()
                .map(participant -> participant.getUserId())
                .filter(userId -> !userId.equals(party.getCreatorId()))
                .distinct()
                .toList();

        webPushSender.sendToUsers(participantUserIds, payload(party, title, bodyFormat));
    }

    private WebPushPayload payload(DeliveryParty party, String title, String bodyFormat) {
        return new WebPushPayload(
                title,
                bodyFormat.formatted(party.getTitle()),
                PARTY_CATEGORY,
                PARTY_URL_FORMAT.formatted(party.getId())
        );
    }
}
