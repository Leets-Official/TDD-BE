package com.leets.tdd.review.dto;

import java.util.List;

public record ReceivedReviewListResponse(
    List<ReceivedReviewResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
}
