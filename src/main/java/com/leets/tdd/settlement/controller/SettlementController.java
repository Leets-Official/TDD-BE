package com.leets.tdd.settlement.controller;

import com.leets.tdd.global.auth.UserPrincipal;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.settlement.dto.request.CreateSettlementRequest;
import com.leets.tdd.settlement.dto.response.MySettlementListResponse;
import com.leets.tdd.settlement.dto.response.PaymentStatusResponse;
import com.leets.tdd.settlement.dto.response.SettlementCancelResponse;
import com.leets.tdd.settlement.dto.response.SettlementCompletionResponse;
import com.leets.tdd.settlement.dto.response.SettlementDetailResponse;
import com.leets.tdd.settlement.exception.SettlementErrorCode;
import com.leets.tdd.settlement.exception.SettlementException;
import com.leets.tdd.settlement.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SettlementController {

  private final SettlementService settlementService;

  @PostMapping("/parties/{partyId}/settlement")
  public ResponseEntity<ApiResponse<SettlementDetailResponse>> createSettlement(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId,
      @Valid @RequestBody CreateSettlementRequest request
  ) {
    SettlementDetailResponse response = settlementService.createSettlement(currentUserId(userPrincipal), partyId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("정산 요청이 등록되었습니다.", response));
  }

  @GetMapping("/parties/{partyId}/settlement")
  public ResponseEntity<ApiResponse<SettlementDetailResponse>> getSettlement(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    SettlementDetailResponse response = settlementService.getSettlement(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("정산 내역을 조회했습니다.", response));
  }

  @PatchMapping("/parties/{partyId}/settlement/payments/me")
  public ResponseEntity<ApiResponse<PaymentStatusResponse>> markMyPaymentPaid(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    PaymentStatusResponse response = settlementService.markMyPaymentPaid(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("송금 완료로 처리했습니다.", response));
  }

  @PatchMapping("/parties/{partyId}/settlement/payments/me/undo")
  public ResponseEntity<ApiResponse<PaymentStatusResponse>> undoMyPaymentPaid(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    PaymentStatusResponse response = settlementService.undoMyPaymentPaid(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("송금 완료를 취소했습니다.", response));
  }

  @PatchMapping("/parties/{partyId}/settlement/complete")
  public ResponseEntity<ApiResponse<SettlementCompletionResponse>> completeSettlement(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    SettlementCompletionResponse response = settlementService.completeSettlement(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("정산을 완료했습니다.", response));
  }

  @PatchMapping("/parties/{partyId}/settlement/cancel")
  public ResponseEntity<ApiResponse<SettlementCancelResponse>> cancelSettlement(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    SettlementCancelResponse response = settlementService.cancelSettlement(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("정산 요청을 취소했습니다.", response));
  }

  @GetMapping("/users/me/settlements")
  public ResponseEntity<ApiResponse<MySettlementListResponse>> getMySettlements(
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    MySettlementListResponse response = settlementService.getMySettlements(currentUserId(userPrincipal));
    return ResponseEntity.ok(ApiResponse.success("내 정산 현황을 조회했습니다.", response));
  }

  private Long currentUserId(UserPrincipal userPrincipal) {
    if (userPrincipal == null || userPrincipal.userId() == null) {
      throw new SettlementException(SettlementErrorCode.UNAUTHORIZED);
    }
    return userPrincipal.userId();
  }
}
