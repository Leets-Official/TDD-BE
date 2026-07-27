package com.leets.tdd.party.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantRole;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.MyPartyStatusFilter;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.JoinDeliveryPartyResponse;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import com.leets.tdd.user.repository.DormitoryRepository;
import com.leets.tdd.user.domain.Dormitory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryPartyService {

    private final DeliveryPartyRepository deliveryPartyRepository;
    private final PartyParticipantRepository partyParticipantRepository;
    private final UserRepository userRepository;
    private final DormitoryRepository dormitoryRepository;


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
    public List<CreateDeliveryPartyResponse> getDeliveryParties(
            Long categoryId,
            String dormitory,
            LocalDateTime orderExpectedFrom,
            LocalDateTime orderExpectedTo
    ) {
        List<DeliveryParty> recruitingParties = deliveryPartyRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(party -> party.getStatus() == PartyStatus.RECRUITING)
                .toList();

        return filterAndMapParties(
                recruitingParties,
                categoryId,
                dormitory,
                orderExpectedFrom,
                orderExpectedTo,
                party -> true
        );
    }


    // 내 배달팟 목록 조회 API - 참여 중인 사용자 기준
    public List<CreateDeliveryPartyResponse> getMyDeliveryParties(
            Long currentUserId,
            Long categoryId,
            String dormitory,
            LocalDateTime orderExpectedFrom,
            LocalDateTime orderExpectedTo,
            MyPartyStatusFilter statusFilter
    ) {
        Set<Long> partyIds = partyParticipantRepository
                .findAllByUserIdAndStatus(currentUserId, PartyParticipantStatus.JOINED)
                .stream()
                .map(PartyParticipant::getPartyId)
                .collect(Collectors.toSet());

        if (partyIds.isEmpty()) {
            return List.of();
        }

        List<DeliveryParty> parties = deliveryPartyRepository.findAllById(partyIds)
                .stream()
                .sorted((first, second) -> second.getCreatedAt().compareTo(first.getCreatedAt()))
                .toList();

        return filterAndMapParties(
                parties,
                categoryId,
                dormitory,
                orderExpectedFrom,
                orderExpectedTo,
                party -> matchesMyPartyStatus(party, statusFilter)
        );
    }

    private List<CreateDeliveryPartyResponse> filterAndMapParties(
            List<DeliveryParty> parties,
            Long categoryId,
            String dormitory,
            LocalDateTime orderExpectedFrom,
            LocalDateTime orderExpectedTo,
            Predicate<DeliveryParty> statusCondition
    ) {
        Map<Long, String> dormitoryByUserId = dormitoryByUserId(parties.stream()
                .map(DeliveryParty::getCreatorId)
                .collect(Collectors.toSet()));

        return parties.stream()
                .filter(statusCondition)
                .filter(party -> categoryId == null || party.getFoodCategoryId().equals(categoryId))
                .filter(party -> dormitory == null || dormitory.equals(dormitoryByUserId.get(party.getCreatorId())))
                .filter(party -> orderExpectedFrom == null
                        || !party.getOrderExpectedAt().isBefore(orderExpectedFrom))
                .filter(party -> orderExpectedTo == null
                        || !party.getOrderExpectedAt().isAfter(orderExpectedTo))
                .map(this::toCreateDeliveryPartyResponse)
                .toList();
    }

    private Map<Long, String> dormitoryByUserId(Collection<Long> userIds) {
        return dormitoryRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(Dormitory::getUserId, Dormitory::getDormitory));
    }

    private boolean matchesMyPartyStatus(DeliveryParty party, MyPartyStatusFilter statusFilter) {
        return switch (statusFilter) {
            case ALL -> party.getStatus() != PartyStatus.CANCELED;
            case IN_PROGRESS -> party.getStatus() == PartyStatus.RECRUITING
                    || party.getStatus() == PartyStatus.CLOSED
                    || party.getStatus() == PartyStatus.ORDERED;
            case COMPLETED -> party.getStatus() == PartyStatus.COMPLETED;
        };
    }

    private CreateDeliveryPartyResponse toCreateDeliveryPartyResponse(DeliveryParty deliveryParty) {
        return new CreateDeliveryPartyResponse(
                deliveryParty.getId(),
                deliveryParty.getFoodCategoryId(),
                deliveryParty.getTitle(),
                deliveryParty.getDescription(),
                deliveryParty.getMinParticipants(),
                deliveryParty.getMaxParticipants(),
                deliveryParty.getOrderExpectedAt(),
                deliveryParty.getStatus().name(),
                deliveryParty.getCreatedAt()
        );
    }


    // 배달팟 상세 조회 API
    public DeliveryPartyDetailResponse getDeliveryPartyDetail(Long partyId) {

        DeliveryParty deliveryParty = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        return new DeliveryPartyDetailResponse(deliveryParty);
    }


    // 배달팟 참여 API
    @Transactional
    public JoinDeliveryPartyResponse joinDeliveryParty(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.RECRUITMENT_CLOSED);
        }

        // 방장은 생성 시점부터 참여 인원으로 간주한다.
        if (deliveryParty.getCreatorId().equals(currentUserId)
                || partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
                        partyId, currentUserId, PartyParticipantStatus.JOINED)) {
            throw new PartyException(PartyErrorCode.ALREADY_JOINED);
        }

        long currentParticipants = currentParticipants(partyId);
        if (currentParticipants >= deliveryParty.getMaxParticipants()) {
            throw new PartyException(PartyErrorCode.PARTY_FULL);
        }

        try {
            partyParticipantRepository.saveAndFlush(new PartyParticipant(
                    partyId,
                    currentUserId,
                    PartyParticipantRole.MEMBER,
                    PartyParticipantStatus.JOINED,
                    LocalDateTime.now()
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new PartyException(PartyErrorCode.JOIN_FAILED);
        }

        return new JoinDeliveryPartyResponse(
                partyId,
                currentParticipants + 1,
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


        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.NOT_OWNER);
        }


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


    // 배달팟 삭제(취소) API
    public Long deleteDeliveryParty(
            Long partyId,
            Long currentUserId
    ) {

        DeliveryParty deliveryParty = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.DELETE_FORBIDDEN);
        }

        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.CANCEL_NOT_RECRUITING);
        }

        deliveryParty.cancel();

        deliveryPartyRepository.save(deliveryParty);

        return partyId;
    }
}
