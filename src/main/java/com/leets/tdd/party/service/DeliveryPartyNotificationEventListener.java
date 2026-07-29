package com.leets.tdd.party.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 커밋된 배달팟 상태 변경에 대해서만 웹푸시를 발송한다. */
@Component
@RequiredArgsConstructor
public class DeliveryPartyNotificationEventListener {

    private final DeliveryPartyNotifier deliveryPartyNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DeliveryPartyNotificationEvent event) {
        switch (event.type()) {
            case PARTICIPANT_JOINED -> deliveryPartyNotifier.notifyParticipantJoined(event.party());
            case PARTICIPANT_LEFT -> deliveryPartyNotifier.notifyParticipantLeft(event.party());
            case RECRUITMENT_CLOSED -> deliveryPartyNotifier.notifyRecruitmentClosed(event.party());
            case ORDER_COMPLETED -> deliveryPartyNotifier.notifyOrderCompleted(event.party());
            case DELIVERY_COMPLETED -> deliveryPartyNotifier.notifyDeliveryCompleted(event.party());
            case PARTY_CANCELED -> deliveryPartyNotifier.notifyPartyCanceled(event.party());
        }
    }
}
