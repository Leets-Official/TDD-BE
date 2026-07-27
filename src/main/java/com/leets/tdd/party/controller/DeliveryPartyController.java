package com.leets.tdd.party.controller;

import java.util.List;
import java.time.LocalDateTime;

import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.MyPartyStatusFilter;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.JoinDeliveryPartyResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;


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
    public ResponseEntity<List<CreateDeliveryPartyResponse>> getDeliveryParties(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String dormitory,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime orderExpectedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime orderExpectedTo
    ) {

        List<CreateDeliveryPartyResponse> response =
                deliveryPartyService.getDeliveryParties(
                        categoryId, dormitory, orderExpectedFrom, orderExpectedTo);

        return ResponseEntity.ok(response);
    }


    // 내 배달팟 목록 조회 API - 참여자 기준
    @GetMapping("/me")
    public ResponseEntity<List<CreateDeliveryPartyResponse>> getMyDeliveryParties(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String dormitory,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime orderExpectedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime orderExpectedTo,
            @RequestParam(defaultValue = "ALL") MyPartyStatusFilter statusFilter
    ) {
        List<CreateDeliveryPartyResponse> response = deliveryPartyService.getMyDeliveryParties(
                currentUser.userId(),
                categoryId,
                dormitory,
                orderExpectedFrom,
                orderExpectedTo,
                statusFilter
        );

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
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {

        deliveryPartyService.updateDeliveryParty(
                partyId,
                request,
                currentUser.userId()
        );

        return ResponseEntity.ok().build();
    }


    // 배달팟 삭제(취소) API
    @DeleteMapping("/{partyId}")
    @Operation(
            summary = "배달팟 모집 취소",
            description = "파티장이 모집 중인 배달팟을 취소하고 상태를 CANCELED로 변경합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "배달팟 모집 취소 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "모집 중인 배달팟이 아님"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "파티장이 아님"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "배달팟을 찾을 수 없음"
            )
    })
    public ResponseEntity<Long> deleteDeliveryParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {

        Long deletedPartyId =
                deliveryPartyService.deleteDeliveryParty(
                        partyId,
                        currentUser.userId()
                );

        return ResponseEntity.ok(deletedPartyId);
    }
}
