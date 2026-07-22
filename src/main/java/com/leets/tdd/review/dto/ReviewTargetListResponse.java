package com.leets.tdd.review.dto;

import java.util.List;

public record ReviewTargetListResponse(
    Long partyId,
    List<ReviewTargetResponse> targets
) {
}
