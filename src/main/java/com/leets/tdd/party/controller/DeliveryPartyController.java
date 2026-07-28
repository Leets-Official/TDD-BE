package com.leets.tdd.party.controller;

import java.time.LocalDateTime;

import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.global.jwt.UserPrincipal;
import com.leets.tdd.party.dto.MyPartyStatusFilter;
import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeleteDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.MyDeliveryPartyListResponse;
import com.leets.tdd.party.dto.response.RecruitingDeliveryPartyListResponse;
import com.leets.tdd.party.service.DeliveryPartyService;
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


    @Operation(
            summary = "메인 배달팟 목록 조회",
            description = "모집 중인 배달팟을 카테고리, 파티장 기숙사, 주문 예정 시간으로 필터링해 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 목록 조회 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<RecruitingDeliveryPartyListResponse>> getDeliveryParties(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long dormitoryId,
            @RequestParam(required = false) LocalDateTime orderExpectedFrom,
            @RequestParam(required = false) LocalDateTime orderExpectedTo
    ) {
        RecruitingDeliveryPartyListResponse response = deliveryPartyService.getRecruitingDeliveryParties(
                categoryId, dormitoryId, orderExpectedFrom, orderExpectedTo
        );
        return ResponseEntity.ok(ApiResponse.success("배달팟 목록 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "내 배달팟 목록 조회",
            description = "참여 중인 배달팟을 상태, 카테고리, 파티장 기숙사, 주문 예정 시간으로 필터링해 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 배달팟 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "내 배달팟 목록 조회 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyDeliveryPartyListResponse>> getMyDeliveryParties(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "ALL") MyPartyStatusFilter status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long dormitoryId,
            @RequestParam(required = false) LocalDateTime orderExpectedFrom,
            @RequestParam(required = false) LocalDateTime orderExpectedTo
    ) {
        MyDeliveryPartyListResponse response = deliveryPartyService.getMyDeliveryParties(
                currentUser.userId(), status, categoryId, dormitoryId, orderExpectedFrom, orderExpectedTo
        );
        return ResponseEntity.ok(ApiResponse.success("내 배달팟 목록 조회에 성공했습니다.", response));
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
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {

        deliveryPartyService.updateDeliveryParty(
                partyId,
                request,
                userPrincipal.userId()
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 모집 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "취소할 수 없는 배달팟 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티장이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟을 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<DeleteDeliveryPartyResponse>> deleteDeliveryParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {

        Long deletedPartyId =
                deliveryPartyService.deleteDeliveryParty(
                        partyId,
                        userPrincipal.userId()
                );

        return ResponseEntity.ok(ApiResponse.success(
                "배달팟이 삭제되었습니다.",
                new DeleteDeliveryPartyResponse(deletedPartyId)
        ));
    }
}
