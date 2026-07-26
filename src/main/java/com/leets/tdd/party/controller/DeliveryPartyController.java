package com.leets.tdd.party.controller;

import java.util.List;

import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.JoinDeliveryPartyResponse;
import com.leets.tdd.party.service.DeliveryPartyService;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.global.jwt.UserPrincipal;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping({"/api/v1/parties", "/api/v1/delivery-parties"})
@RequiredArgsConstructor
public class DeliveryPartyController {

    private final DeliveryPartyService deliveryPartyService;


    // 배달팟 생성 API
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


    // 배달팟 목록 조회 API
    @GetMapping
    public ResponseEntity<List<CreateDeliveryPartyResponse>> getDeliveryParties() {

        List<CreateDeliveryPartyResponse> response =
                deliveryPartyService.getDeliveryParties();

        return ResponseEntity.ok(response);
    }


    // 배달팟 상세 조회 API
    @GetMapping("/{partyId}")
    public ResponseEntity<DeliveryPartyDetailResponse> getDeliveryPartyDetail(
            @PathVariable Long partyId
    ) {

        DeliveryPartyDetailResponse response =
                deliveryPartyService.getDeliveryPartyDetail(partyId);

        return ResponseEntity.ok(response);
    }


    // 배달팟 참여 API
    @PostMapping("/{partyId}/join")
    public ResponseEntity<ApiResponse<JoinDeliveryPartyResponse>> joinDeliveryParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        JoinDeliveryPartyResponse response = deliveryPartyService.joinDeliveryParty(
                partyId,
                currentUser.userId()
        );

        return ResponseEntity.ok(ApiResponse.success("배달팟에 참여했습니다.", response));
    }


    // 배달팟 수정 API
    @PutMapping("/{partyId}")
    public ResponseEntity<Void> updateDeliveryParty(
            @PathVariable Long partyId,
            @RequestBody UpdateDeliveryPartyRequest request,
            @AuthenticationPrincipal Long currentUserId
    ) {

        deliveryPartyService.updateDeliveryParty(
                partyId,
                request,
                currentUserId
        );

        return ResponseEntity.ok().build();
    }


    // 배달팟 삭제(취소) API
    @DeleteMapping("/{partyId}")
    public ResponseEntity<Long> deleteDeliveryParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal Long currentUserId
    ) {

        Long deletedPartyId =
                deliveryPartyService.deleteDeliveryParty(
                        partyId,
                        currentUserId
                );

        return ResponseEntity.ok(deletedPartyId);
    }
}
