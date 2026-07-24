package com.leets.tdd.report.dto;

import com.leets.tdd.report.domain.Report;
import java.time.LocalDateTime;

public record CreateReportResponse(
    Long reportId,
    Long partyId,
    Long reportedUserId,
    String reason,
    String status,
    LocalDateTime createdAt
) {

  public static CreateReportResponse from(Report report) {
    return new CreateReportResponse(
        report.getId(),
        report.getPartyId(),
        report.getReportedUserId(),
        report.getReason().name(),
        report.getStatus().name(),
        report.getCreatedAt()
    );
  }
}
