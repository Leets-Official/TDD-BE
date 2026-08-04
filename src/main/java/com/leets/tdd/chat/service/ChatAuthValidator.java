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
 * 채팅 접근 조건 = 팟이 종료(CANCELED/DELIVERED/SETTLED) 상태가 아니고, 그 팟의 방장이거나 JOINED 참여자.

 * 상태 검사를 메시지 저장과 같은 트랜잭션·row lock 안에서 수행해야 하는 경로(ChatService.saveMessage)를
 * 위해, 이미 조회(락)된 DeliveryParty를 받는 오버로드를 제공한다. 락이 필요 없는 경로(이미지 presign/confirm 등)는
 * partyId를 받는 오버로드가 내부에서 팟을 조회한다.
 */
@Component
@RequiredArgsConstructor
public class ChatAuthValidator {

    private final DeliveryPartyRepository deliveryPartyRepository;
    private final PartyParticipantRepository partyParticipantRepository;

    /**
     * 팟을 직접 조회해 채팅 접근 권한을 검증한다. 통과하지 못하면 예외를 던진다.
     * 락이 필요 없는 경로(이미지 업로드 등)에서 사용한다.
     */
    public void validateChatAccess(Long partyId, Long userId) {
        DeliveryParty party = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new IllegalArgumentException("배달팟을 찾을 수 없습니다."));
        validateChatAccess(party, userId);
    }

    /**
     * 이미 조회(락)된 팟으로 채팅 접근 권한을 검증한다. 통과하지 못하면 예외를 던진다.
     * 상태 검사를 호출자의 트랜잭션·row lock 범위 안에서 수행해야 할 때 사용한다
     * (ChatService.saveMessage가 findWithLockById로 잠근 팟을 넘긴다).
     */
    public void validateChatAccess(DeliveryParty party, Long userId) {
        if (party.getStatus() == PartyStatus.CANCELED
                || party.getStatus() == PartyStatus.DELIVERED
                || party.getStatus() == PartyStatus.SETTLED) {
            throw new IllegalArgumentException("종료된 배달팟에서는 채팅을 이용할 수 없습니다.");
        }
        if (party.getCreatorId().equals(userId)) {
            return;
        }
        if (!partyParticipantRepository
                .existsByPartyIdAndUserIdAndStatus(party.getId(), userId, PartyParticipantStatus.JOINED)) {
            throw new IllegalArgumentException("해당 팟의 참여자만 채팅에 접근할 수 있습니다.");
        }
    }
}
