package com.leets.tdd.deliveryparty.service;

import com.leets.tdd.deliveryparty.domain.DeliveryPartyStatus;
import com.leets.tdd.deliveryparty.domain.DeliveryParty;
import com.leets.tdd.deliveryparty.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.deliveryparty.repository.DeliveryPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeliveryPartyService {

    private final DeliveryPartyRepository deliveryPartyRepository;

    public void createDeliveryParty(CreateDeliveryPartyRequest request) {

        DeliveryParty deliveryParty = new DeliveryParty(
                request.getFoodCategoryId(),
                request.getTitle(),
                request.getDescription(),
                request.getMinParticipants(),
                request.getMaxParticipants(),
                request.getOrderExpectedAt(),
                DeliveryPartyStatus.RECRUITING,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        deliveryPartyRepository.save(deliveryParty);
    }
}
