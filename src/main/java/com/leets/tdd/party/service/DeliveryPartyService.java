package com.leets.tdd.party.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.LeaveDeliveryPartyResponse;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryPartyService {

    private final DeliveryPartyRepository deliveryPartyRepository;
    private final PartyParticipantRepository partyParticipantRepository;
    private final UserRepository userRepository;


    // 배달팟 생성 API
    public CreateDeliveryPartyResponse createDeliveryParty(CreateDeliveryPartyRequest request) {

        User user = userRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("사용자가 없습니다."));

        Long creatorId = user.getId();

        DeliveryParty deliveryParty = new DeliveryParty(
                creatorId,
                request.getFoodCategoryId(),
                request.getTitle(),
                request.getDescription(),
                request.getMinParticipants(),
                request.getMaxParticipants(),
                request.getOrderExpectedAt(),
                PartyStatus.RECRUITING,
                null,
                SettlementStatus.NONE,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        DeliveryParty savedDeliveryParty = deliveryPartyRepository.save(deliveryParty);

        return new CreateDeliveryPartyResponse(
                savedDeliveryParty.getId(),
                savedDeliveryParty.getFoodCategoryId(),
                savedDeliveryParty.getTitle(),
                savedDeliveryParty.getDescription(),
                savedDeliveryParty.getMinParticipants(),
                savedDeliveryParty.getMaxParticipants(),
                savedDeliveryParty.getOrderExpectedAt(),
                savedDeliveryParty.getStatus().name(),
                savedDeliveryParty.getCreatedAt()
        );
    }


    // 배달팟 목록 조회 API
    public List<CreateDeliveryPartyResponse> getDeliveryParties() {

        List<DeliveryParty> deliveryParties =
                deliveryPartyRepository.findAllByOrderByCreatedAtDesc();

        return deliveryParties.stream()
                .map(deliveryParty -> new CreateDeliveryPartyResponse(
                        deliveryParty.getId(),
                        deliveryParty.getFoodCategoryId(),
                        deliveryParty.getTitle(),
                        deliveryParty.getDescription(),
                        deliveryParty.getMinParticipants(),
                        deliveryParty.getMaxParticipants(),
                        deliveryParty.getOrderExpectedAt(),
                        deliveryParty.getStatus().name(),
                        deliveryParty.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }


    // 배달팟 상세 조회 API
    public DeliveryPartyDetailResponse getDeliveryPartyDetail(Long partyId) {

        DeliveryParty deliveryParty = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        return new DeliveryPartyDetailResponse(deliveryParty);
    }


    // 배달팟 참여 취소 API
    @Transactional
    public LeaveDeliveryPartyResponse leaveDeliveryParty(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.HOST_CANNOT_LEAVE);
        }

        PartyParticipant participant = partyParticipantRepository.findByPartyIdAndUserId(partyId, currentUserId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.NOT_PARTICIPANT));

        if (!participant.isJoined()) {
            throw new PartyException(PartyErrorCode.NOT_PARTICIPANT);
        }

        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.LEAVE_NOT_ALLOWED);
        }

        participant.cancel(LocalDateTime.now());
        try {
            partyParticipantRepository.saveAndFlush(participant);
        } catch (DataIntegrityViolationException exception) {
            throw new PartyException(PartyErrorCode.LEAVE_FAILED);
        }

        return new LeaveDeliveryPartyResponse(
                partyId,
                currentParticipants(partyId),
                deliveryParty.getMaxParticipants()
        );
    }

    private long currentParticipants(Long partyId) {
        return 1 + partyParticipantRepository.countByPartyIdAndStatus(
                partyId, PartyParticipantStatus.JOINED);
    }


    // 배달팟 수정 API
    public void updateDeliveryParty(
            Long partyId,
            UpdateDeliveryPartyRequest request,
            Long currentUserId
    ) {

        DeliveryParty deliveryParty = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));


        // 작성자(파티장)만 수정 가능
        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.NOT_OWNER);
        }

// 모집 중(RECRUITING) 상태에서만 수정 가능
        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.INVALID_PARTY_STATUS);
        }


        deliveryParty.update(
                request.getTitle(),
                request.getDescription(),
                request.getMaxParticipants(),
                request.getOrderExpectedAt()
        );
        deliveryPartyRepository.save(deliveryParty);
    }
}
