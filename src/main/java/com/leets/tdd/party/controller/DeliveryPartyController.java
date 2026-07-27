package com.leets.tdd.party.controller;

import java.util.List;

import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.CloseDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
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

    @Operation(
            summary = "배달팟 모집 마감",
            description = "파티장이 모집 중인 배달팟의 모집을 마감합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모집 마감 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티장 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 모집이 마감됨")
    })
    @PatchMapping("/{partyId}/close")
    public ResponseEntity<ApiResponse<CloseDeliveryPartyResponse>> closeDeliveryParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        CloseDeliveryPartyResponse response = deliveryPartyService.closeDeliveryParty(
                partyId,
                currentUser.userId()
        );

        return ResponseEntity.ok(ApiResponse.success("배달팟 모집이 마감되었습니다.", response));
    }
}
