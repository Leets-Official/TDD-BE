package com.leets.tdd.review.dto;

import java.util.List;

public record ReviewTagListResponse(
    List<ReviewTagResponse> tags
) {
}
