package com.leets.tdd.settlement.controller;

import com.leets.tdd.global.jwt.UserPrincipal;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.settlement.dto.request.CreateSettlementRequest;
import com.leets.tdd.settlement.dto.request.RegisterBankAccountRequest;
import com.leets.tdd.settlement.dto.response.BankAccountResponse;
import com.leets.tdd.settlement.dto.response.MySettlementListResponse;
import com.leets.tdd.settlement.dto.response.PaymentStatusResponse;
import com.leets.tdd.settlement.dto.response.SettlementCancelResponse;
import com.leets.tdd.settlement.dto.response.SettlementCompletionResponse;
import com.leets.tdd.settlement.dto.response.SettlementDetailResponse;
import com.leets.tdd.settlement.exception.SettlementErrorCode;
import com.leets.tdd.settlement.exception.SettlementException;
import com.leets.tdd.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Settlement", description = "배달팟 정산 API")
@SecurityRequirement(name = "bearerAuth")
public class SettlementController {

  private final SettlementService settlementService;

  @PostMapping("/users/me/bank-account")
  @Operation(summary = "계좌 등록", description = "마이페이지에서 정산받을/보낼 본인 명의 계좌를 등록합니다. 이미 등록된 계좌가 있으면 실패합니다.")
  public ResponseEntity<ApiResponse<BankAccountResponse>> registerBankAccount(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody RegisterBankAccountRequest request
  ) {
    BankAccountResponse response = settlementService.registerBankAccount(currentUserId(userPrincipal), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("계좌등록에 성공하였습니다.", response));
  }

  @PatchMapping("/users/me/bank-account")
  @Operation(summary = "계좌 수정", description = "마이페이지에서 등록된 계좌 정보를 재입력해서 수정합니다. 등록된 계좌가 없으면 실패합니다.")
  public ResponseEntity<ApiResponse<BankAccountResponse>> updateBankAccount(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @Valid @RequestBody RegisterBankAccountRequest request
  ) {
    BankAccountResponse response = settlementService.updateBankAccount(currentUserId(userPrincipal), request);
    return ResponseEntity.ok(ApiResponse.success("계좌 정보 수정에 성공하였습니다.", response));
  }

  @PostMapping("/parties/{partyId}/settlement")
  @Operation(summary = "정산 요청 생성", description = "방장이 완료된 배달팟의 정산을 요청합니다. 배달팟에 채팅방이 있으면 방장 계좌가 담긴 SETTLEMENT_REQUEST 시스템 메시지가 함께 발행되고, 채팅방이 없으면 메시지 없이 정산 요청만 처리됩니다.")
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
  @Operation(summary = "정산 상세 조회", description = "배달팟 참여자가 정산 내역을 조회합니다.")
  public ResponseEntity<ApiResponse<SettlementDetailResponse>> getSettlement(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    SettlementDetailResponse response = settlementService.getSettlement(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("정산 내역을 조회했습니다.", response));
  }

  @PatchMapping("/parties/{partyId}/settlement/payments/me")
  @Operation(summary = "내 송금 완료 처리", description = "정산 대상 참여자가 자신의 송금을 완료로 표시합니다.")
  public ResponseEntity<ApiResponse<PaymentStatusResponse>> markMyPaymentPaid(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    PaymentStatusResponse response = settlementService.markMyPaymentPaid(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("송금 완료로 처리했습니다.", response));
  }

  @PatchMapping("/parties/{partyId}/settlement/payments/me/undo")
  @Operation(summary = "내 송금 완료 되돌리기", description = "송금 완료 표시를 진행 중 상태로 되돌립니다.")
  public ResponseEntity<ApiResponse<PaymentStatusResponse>> undoMyPaymentPaid(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    PaymentStatusResponse response = settlementService.undoMyPaymentPaid(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("송금 완료를 취소했습니다.", response));
  }

  @PatchMapping("/parties/{partyId}/settlement/complete")
  @Operation(summary = "정산 완료", description = "방장이 진행 중인 정산을 완료합니다. 방장을 제외한 정산 대상 참여자에게 웹푸시 알림을 발송합니다.")
  public ResponseEntity<ApiResponse<SettlementCompletionResponse>> completeSettlement(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    SettlementCompletionResponse response = settlementService.completeSettlement(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("정산을 완료했습니다.", response));
  }

  @PatchMapping("/parties/{partyId}/settlement/cancel")
  @Operation(summary = "정산 취소", description = "방장이 진행 중인 정산 요청을 취소합니다.")
  public ResponseEntity<ApiResponse<SettlementCancelResponse>> cancelSettlement(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    SettlementCancelResponse response = settlementService.cancelSettlement(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("정산 요청을 취소했습니다.", response));
  }

  @GetMapping("/users/me/settlements")
  @Operation(summary = "내 정산 현황 조회", description = "내가 송금할 정산과 방장으로서 받을 정산을 조회합니다.")
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
