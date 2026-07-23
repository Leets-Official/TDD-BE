package com.leets.tdd.party.controller;

import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.service.DeliveryPartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery-parties")
@RequiredArgsConstructor
public class DeliveryPartyController {

    private final DeliveryPartyService deliveryPartyService;

    @PostMapping
    public ResponseEntity<CreateDeliveryPartyResponse> createDeliveryParty(
            @RequestBody CreateDeliveryPartyRequest request
    ) {
        CreateDeliveryPartyResponse response =
                deliveryPartyService.createDeliveryParty(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
