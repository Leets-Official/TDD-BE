package com.leets.tdd.chat.service;

import com.leets.tdd.global.webpush.WebPushPayload;
import com.leets.tdd.global.webpush.WebPushSender;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 채팅 도메인의 웹푸시 알림 발송을 담당한다. (정산·배달팟 Notifier와 동일한 컨벤션)
 * 새 메시지가 발행되면 해당 팟의 참여자(발신자 제외)에게 알림을 보낸다.
 */
@Component
@RequiredArgsConstructor
public class ChatNotifier {

    private static final String CHAT_CATEGORY = "CHAT";

    private final WebPushSender webPushSender;
    private final PartyParticipantRepository partyParticipantRepository;

    /**
     * 새 채팅 메시지 알림을 발송한다. 발신자 본인은 제외한다.
     */
    public void notifyNewMessage(Long partyId, Long senderId) {
        List<Long> targetUserIds = partyParticipantRepository
                .findAllByPartyIdAndStatus(partyId, PartyParticipantStatus.JOINED)
                .stream()
                .map(PartyParticipant::getUserId)
                .filter(userId -> !userId.equals(senderId))
                .distinct()
                .toList();

        if (targetUserIds.isEmpty()) {
            return;
        }

        webPushSender.sendToUsers(
                targetUserIds,
                new WebPushPayload(
                        "새 메시지",
                        "새로운 채팅 메시지가 도착했어요.",
                        CHAT_CATEGORY,
                        "/parties/" + partyId + "/chat"
                )
        );
    }
}
