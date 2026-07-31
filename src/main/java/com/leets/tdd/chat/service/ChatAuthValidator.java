package com.leets.tdd.chat.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 채팅(메시지·이미지) 접근 권한을 검증한다.
 * 해당 팟의 JOINED 참여자만 채팅을 보낼 수 있으며, 취소 또는 배달 완료된 팟은 전송할 수 없다.
 */
@Component
@RequiredArgsConstructor
public class ChatAuthValidator {

    private final DeliveryPartyRepository deliveryPartyRepository;
    private final PartyParticipantRepository partyParticipantRepository;

    /**
     * 사용자가 해당 팟의 채팅 메시지를 보낼 수 있는지 검증한다. 없으면 예외를 던진다.
     * 취소 또는 배달 완료된 팟에서는 참여 이력을 유지하더라도 메시지 전송을 막는다.
     */
    public void validateChatAccess(Long partyId, Long userId) {
        DeliveryParty party = findParty(partyId);
        if (party.getStatus() == PartyStatus.CANCELED || party.getStatus() == PartyStatus.COMPLETED) {
            throw new IllegalArgumentException("종료된 배달팟에서는 채팅을 보낼 수 없습니다.");
        }
        if (!hasChatAccess(party, partyId, userId)) {
            throw new IllegalArgumentException("해당 팟의 참여자만 채팅에 접근할 수 있습니다.");
        }
    }

    /**
     * 권한 여부를 boolean으로 반환한다(예외 없이 판정만 필요한 경우).
     */
    public boolean hasChatAccess(Long partyId, Long userId) {
        DeliveryParty party = findParty(partyId);
        return hasChatAccess(party, partyId, userId);
    }

    private boolean hasChatAccess(DeliveryParty party, Long partyId, Long userId) {
        if (party.getCreatorId().equals(userId)) {
            return true;
        }
        return partyParticipantRepository
                .existsByPartyIdAndUserIdAndStatus(partyId, userId, PartyParticipantStatus.JOINED);
    }

    private DeliveryParty findParty(Long partyId) {
        return deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("배달팟을 찾을 수 없습니다."));
    }
}
