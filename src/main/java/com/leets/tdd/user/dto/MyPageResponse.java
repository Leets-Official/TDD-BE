package com.leets.tdd.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record MyPageResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        BigDecimal mannerTemperature,
        int noShowApprovedCount,
        LocalDateTime suspendedUntil,
        String status,
        String dormitory,
        String dormStatus,
        LocalDateTime dormVerifiedAt,
        LocalDateTime dormVerifiedUntil,
        String rejectReason
) {
}
