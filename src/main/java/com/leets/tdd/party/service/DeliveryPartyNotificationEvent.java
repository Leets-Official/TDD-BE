package com.leets.tdd.party.service;

import com.leets.tdd.party.domain.DeliveryParty;

/** 트랜잭션이 성공적으로 커밋된 뒤 웹푸시를 발송하기 위한 도메인 이벤트다. */
public record DeliveryPartyNotificationEvent(
        DeliveryParty party,
        DeliveryPartyNotificationType type
) {
}
