package com.leets.tdd.party.dto.response;

import java.util.List;

public record DeliveryPartySearchResponse(
        List<DeliveryPartySearchItemResponse> parties
) {
}
