package com.leets.tdd.report.repository;

import com.leets.tdd.report.domain.Report;
import com.leets.tdd.report.domain.ReportReason;
import com.leets.tdd.report.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

  boolean existsByPartyIdAndReporterIdAndReportedUserIdAndReasonAndStatus(
      Long partyId,
      Long reporterId,
      Long reportedUserId,
      ReportReason reason,
      ReportStatus status
  );
}
