package com.leets.tdd.party.dto.response;

import java.util.List;

public record MyDeliveryPartyListResponse(
        List<MyDeliveryPartyResponse> parties
) {
}
