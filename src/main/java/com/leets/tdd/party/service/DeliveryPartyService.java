package com.leets.tdd.party.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryPartyService {

    private final DeliveryPartyRepository deliveryPartyRepository;


    // 배달팟 생성 API
    public CreateDeliveryPartyResponse createDeliveryParty(CreateDeliveryPartyRequest request) {

        DeliveryParty deliveryParty = new DeliveryParty(
                1L, // TODO: 로그인 사용자 ID로 변경
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
}
