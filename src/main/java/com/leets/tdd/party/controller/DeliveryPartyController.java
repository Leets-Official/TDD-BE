package com.leets.tdd.party.controller;

import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.service.DeliveryPartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/delivery-parties")
@RequiredArgsConstructor
public class DeliveryPartyController {

    private final DeliveryPartyService deliveryPartyService;

    @PostMapping
    public void createDeliveryParty(
            @RequestBody CreateDeliveryPartyRequest request
    ) {
        deliveryPartyService.createDeliveryParty(request);
    }
}
