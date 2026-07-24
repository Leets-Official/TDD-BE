package com.leets.tdd.settlement.dto.response;

import java.util.List;

public record MySettlementListResponse(
    MySettlementSummaryResponse summary,
    List<MyOutgoingSettlementResponse> outgoing,
    List<MyIncomingSettlementResponse> incoming
) {
}
