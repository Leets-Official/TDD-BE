package com.leets.tdd.party.controller;

import java.util.List;

import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.PartyParticipantListResponse;
import com.leets.tdd.party.service.DeliveryPartyService;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.global.jwt.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping({"/api/v1/delivery-parties", "/api/v1/parties"})
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


    // 배달팟 참여자 목록 조회 API
    @GetMapping("/{partyId}/participants")
    @Operation(
            summary = "배달팟 참여자 목록 조회",
            description = "로그인한 사용자가 배달팟의 참여자 목록을 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "참여자 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "배달팟을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500", description = "참여자 목록 조회 실패"
            )
    })
    public ResponseEntity<ApiResponse<PartyParticipantListResponse>> getPartyParticipants(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        PartyParticipantListResponse response = deliveryPartyService.getPartyParticipants(partyId);
        return ResponseEntity.ok(ApiResponse.success("참여자 목록 조회에 성공했습니다.", response));
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
}
