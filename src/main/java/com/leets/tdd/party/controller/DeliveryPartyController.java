package com.leets.tdd.party.controller;

import java.time.LocalDateTime;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.global.jwt.UserPrincipal;
import com.leets.tdd.party.dto.MyPartyStatusFilter;
import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CompleteDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.CloseDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.DeliveryPartySearchResponse;
import com.leets.tdd.party.dto.response.DeleteDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.JoinDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.LeaveDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.MyDeliveryPartyListResponse;
import com.leets.tdd.party.dto.response.OrderDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.PartyParticipantListResponse;
import com.leets.tdd.party.dto.response.RecruitingDeliveryPartyListResponse;
import com.leets.tdd.party.service.DeliveryPartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
@Tag(name = "Delivery Party", description = "배달팟 생성, 조회, 참여 및 상태 변경 API")
public class DeliveryPartyController {

    private final DeliveryPartyService deliveryPartyService;


    // 배달팟 생성 API
    @Operation(
            summary = "배달팟 생성",
            description = "로그인한 사용자가 배달팟을 생성하고 방장 및 첫 번째 참여자로 등록됩니다. "
                    + "기숙사는 1기숙사, 2기숙사, 3기숙사 중 하나를 필수로 선택합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "배달팟 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "음식 카테고리 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 생성 실패")
    })
    @PostMapping
    public ResponseEntity<CreateDeliveryPartyResponse> createDeliveryParty(
            @Valid @RequestBody CreateDeliveryPartyRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        CreateDeliveryPartyResponse response =
                deliveryPartyService.createDeliveryParty(request, currentUser.userId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @Operation(
            summary = "메인 배달팟 목록 조회",
            description = "모집 중인 배달팟을 카테고리, 배달팟 기숙사, 주문 예정 시간으로 필터링해 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 필터 조건"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 목록 조회 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<RecruitingDeliveryPartyListResponse>> getDeliveryParties(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String dormitory,
            @RequestParam(required = false) LocalDateTime orderExpectedFrom,
            @RequestParam(required = false) LocalDateTime orderExpectedTo
    ) {
        RecruitingDeliveryPartyListResponse response = deliveryPartyService.getRecruitingDeliveryParties(
                categoryId, dormitory, orderExpectedFrom, orderExpectedTo
        );
        return ResponseEntity.ok(ApiResponse.success("배달팟 목록 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "내 배달팟 목록 조회",
            description = "참여 중인 배달팟을 상태, 카테고리, 배달팟 기숙사, 주문 예정 시간으로 필터링해 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내 배달팟 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 필터 조건"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "내 배달팟 목록 조회 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyDeliveryPartyListResponse>> getMyDeliveryParties(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "ALL") MyPartyStatusFilter status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String dormitory,
            @RequestParam(required = false) LocalDateTime orderExpectedFrom,
            @RequestParam(required = false) LocalDateTime orderExpectedTo
    ) {
        MyDeliveryPartyListResponse response = deliveryPartyService.getMyDeliveryParties(
                currentUser.userId(), status, categoryId, dormitory, orderExpectedFrom, orderExpectedTo
        );
        return ResponseEntity.ok(ApiResponse.success("내 배달팟 목록 조회에 성공했습니다.", response));
    }

    @Operation(summary = "배달팟 검색", description = "검색어가 포함된 배달팟 제목을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검색어 미입력"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "검색 결과 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 검색 실패")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<DeliveryPartySearchResponse>> searchDeliveryParties(
            @RequestParam(required = false) String keyword
    ) {
        DeliveryPartySearchResponse response = deliveryPartyService.searchDeliveryParties(keyword);
        return ResponseEntity.ok(ApiResponse.success("배달팟 검색에 성공했습니다.", response));
    }


    // 배달팟 상세 조회 API
    @Operation(summary = "배달팟 상세 조회", description = "배달팟의 상세 정보, 선택된 기숙사 위치와 현재 상태를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 상세 조회 실패")
    })
    @GetMapping("/{partyId}")
    public ResponseEntity<DeliveryPartyDetailResponse> getDeliveryPartyDetail(
            @PathVariable Long partyId
    ) {

        DeliveryPartyDetailResponse response =
                deliveryPartyService.getDeliveryPartyDetail(partyId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "배달팟 참여자 목록 조회", description = "배달팟에 참여 중인 사용자 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참여자 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "참여자 목록 조회 실패")
    })
    @GetMapping("/{partyId}/participants")
    public ResponseEntity<ApiResponse<PartyParticipantListResponse>> getPartyParticipants(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        PartyParticipantListResponse response = deliveryPartyService.getPartyParticipants(partyId);
        return ResponseEntity.ok(ApiResponse.success("참여자 목록 조회에 성공했습니다.", response));
    }

    @Operation(summary = "배달팟 참여", description = "모집 중인 배달팟에 참여합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 참여 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "모집이 종료된 배달팟"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 참여했거나 모집 인원 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 참여 실패")
    })
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

    @Operation(summary = "배달팟 삭제", description = "파티장이 모집 중인 배달팟을 취소하고 CANCELED 상태로 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "삭제할 수 없는 배달팟 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티장이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 삭제 실패")
    })
    @DeleteMapping("/{partyId}")
    public ResponseEntity<ApiResponse<DeleteDeliveryPartyResponse>> deleteDeliveryParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long deletedPartyId = deliveryPartyService.deleteDeliveryParty(partyId, currentUser.userId());
        return ResponseEntity.ok(ApiResponse.success(
                "배달팟이 삭제되었습니다.",
                new DeleteDeliveryPartyResponse(deletedPartyId)
        ));
    }

    @Operation(summary = "배달팟 참여 취소", description = "참여 중인 배달팟에서 나갑니다. 파티장은 참여를 취소할 수 없습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 참여 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "참여 취소할 수 없는 상태 또는 참여자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티장 참여 취소 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 참여 취소 실패")
    })
    @DeleteMapping("/{partyId}/participants")
    public ResponseEntity<ApiResponse<LeaveDeliveryPartyResponse>> leaveDeliveryParty(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        LeaveDeliveryPartyResponse response = deliveryPartyService.leaveDeliveryParty(
                partyId,
                currentUser.userId()
        );
        return ResponseEntity.ok(ApiResponse.success("배달팟 참여가 취소되었습니다.", response));
    }


    // 배달팟 수정 API
    @Operation(hidden = true)
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

    @Operation(summary = "배달 도착", description = "파티장이 주문 완료된 배달팟을 DELIVERED 상태로 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달 도착 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "주문 완료 상태가 아니거나 이미 배달 도착 처리됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티장이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달 완료 처리 실패")
    })
    @PatchMapping("/{partyId}/complete")
    public ResponseEntity<ApiResponse<CompleteDeliveryPartyResponse>> completeDelivery(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        CompleteDeliveryPartyResponse response = deliveryPartyService.completeDelivery(
                partyId,
                currentUser.userId()
        );

        return ResponseEntity.ok(ApiResponse.success("배달이 도착했습니다.", response));
    }

    @Operation(
            summary = "배달팟 모집 마감",
            description = "목표 인원 도달 시 자동 마감됩니다. 방장은 참여 인원 2명 이상인 모집 중 배달팟을 조기 마감할 수 있습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배달팟 모집 마감 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 모집이 마감됐거나 참여 인원이 2명 미만인 배달팟"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티장이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배달팟 모집 마감 실패")
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

    @Operation(summary = "주문 완료", description = "파티장이 모집 마감된 배달팟을 주문 완료 상태로 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "주문 완료 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "모집 마감 상태가 아니거나 이미 주문 완료됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "파티장이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배달팟 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "주문 완료 처리 실패")
    })
    @PatchMapping("/{partyId}/order")
    public ResponseEntity<ApiResponse<OrderDeliveryPartyResponse>> completeOrder(
            @PathVariable Long partyId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        OrderDeliveryPartyResponse response = deliveryPartyService.completeOrder(
                partyId,
                currentUser.userId()
        );

        return ResponseEntity.ok(ApiResponse.success("주문이 완료되었습니다.", response));
    }
}
