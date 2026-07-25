package com.leets.tdd.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateReviewRequest(
    @NotNull Long revieweeId,
    @NotNull @Min(1) @Max(5) Integer rating,
    List<Long> tagIds,
    @Size(max = 500) String content
) {
}
