package com.leets.tdd.settlement.service;

import com.leets.tdd.settlement.dto.request.CreateSettlementRequest;
import com.leets.tdd.settlement.dto.response.MySettlementListResponse;
import com.leets.tdd.settlement.dto.response.PaymentStatusResponse;
import com.leets.tdd.settlement.dto.response.SettlementCancelResponse;
import com.leets.tdd.settlement.dto.response.SettlementCompletionResponse;
import com.leets.tdd.settlement.dto.response.SettlementDetailResponse;

public interface SettlementService {

  SettlementDetailResponse createSettlement(Long currentUserId, Long partyId, CreateSettlementRequest request);

  SettlementDetailResponse getSettlement(Long currentUserId, Long partyId);

  PaymentStatusResponse markMyPaymentPaid(Long currentUserId, Long partyId);

  PaymentStatusResponse undoMyPaymentPaid(Long currentUserId, Long partyId);

  SettlementCompletionResponse completeSettlement(Long currentUserId, Long partyId);

  SettlementCancelResponse cancelSettlement(Long currentUserId, Long partyId);

  MySettlementListResponse getMySettlements(Long currentUserId);
}
