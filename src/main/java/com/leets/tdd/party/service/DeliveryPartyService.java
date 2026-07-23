package com.leets.tdd.party.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.leets.tdd.settlement.domain.SettlementStatus;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeliveryPartyService {

    private final DeliveryPartyRepository deliveryPartyRepository;

    public void createDeliveryParty(CreateDeliveryPartyRequest request) {

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

        deliveryPartyRepository.save(deliveryParty);
    }
}
