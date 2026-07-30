package com.leets.tdd.chat.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 채팅(메시지·이미지) 접근 권한을 검증한다.
 * 해당 팟의 방장(creator)이거나 JOINED 참여자여야 채팅에 접근할 수 있다.
 *
 * 방장은 팟 생성 시 party_participants에 참여자로 등록되지 않으므로(참여는 join API 경로로만 쌓임),
 * 참여자 조회만으로는 방장이 자기 팟 채팅에서 배제된다. 그래서 creatorId를 별도로 인정한다.
 */
@Component
@RequiredArgsConstructor
public class ChatAuthValidator {

    private final DeliveryPartyRepository deliveryPartyRepository;
    private final PartyParticipantRepository partyParticipantRepository;

    /**
     * 사용자가 해당 팟의 채팅에 접근할 권한이 있는지 검증한다. 없으면 예외를 던진다.
     * 권한 = 그 팟의 방장이거나, JOINED 상태의 참여자.
     */
    public void validateChatAccess(Long partyId, Long userId) {
        if (!hasChatAccess(partyId, userId)) {
            throw new IllegalArgumentException("해당 팟의 참여자만 채팅에 접근할 수 있습니다.");
        }
    }

    /**
     * 권한 여부를 boolean으로 반환한다(예외 없이 판정만 필요한 경우).
     */
    public boolean hasChatAccess(Long partyId, Long userId) {
        DeliveryParty party = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("배달팟을 찾을 수 없습니다."));

        // 방장은 참여자 row가 없어도 자기 팟 채팅에 접근할 수 있다.
        if (party.getCreatorId().equals(userId)) {
            return true;
        }
        return partyParticipantRepository
                .existsByPartyIdAndUserIdAndStatus(partyId, userId, PartyParticipantStatus.JOINED);
    }
}
