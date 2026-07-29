package com.leets.tdd.party.service;

/** 배달팟 변경 이후 발송할 알림의 종류다. */
public enum DeliveryPartyNotificationType {
    PARTICIPANT_JOINED,
    PARTICIPANT_LEFT,
    RECRUITMENT_CLOSED,
    ORDER_COMPLETED,
    DELIVERY_COMPLETED,
    PARTY_CANCELED
}
