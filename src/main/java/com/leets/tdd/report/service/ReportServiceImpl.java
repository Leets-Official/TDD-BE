package com.leets.tdd.report.service;

import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.report.domain.Report;
import com.leets.tdd.report.domain.ReportStatus;
import com.leets.tdd.report.dto.CreateReportRequest;
import com.leets.tdd.report.dto.CreateReportResponse;
import com.leets.tdd.report.exception.ReportErrorCode;
import com.leets.tdd.report.exception.ReportException;
import com.leets.tdd.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

  private final DeliveryPartyRepository deliveryPartyRepository;
  private final PartyParticipantRepository partyParticipantRepository;
  private final ReportRepository reportRepository;

  @Override
  @Transactional
  public CreateReportResponse createReport(Long reporterId, Long partyId, CreateReportRequest request) {
    deliveryPartyRepository.findWithLockById(partyId)
        .orElseThrow(() -> new ReportException(ReportErrorCode.PARTY_NOT_FOUND));
    validateParticipant(partyId, reporterId, ReportErrorCode.NOT_PARTICIPANT);
    validateReportedUser(reporterId, partyId, request.reportedUserId());

    if (reportRepository.existsByPartyIdAndReporterIdAndReportedUserIdAndReasonAndStatus(
        partyId,
        reporterId,
        request.reportedUserId(),
        request.reason(),
        ReportStatus.PENDING
    )) {
      throw new ReportException(ReportErrorCode.REPORT_ALREADY_EXISTS);
    }

    Report report = reportRepository.save(Report.create(
        partyId,
        reporterId,
        request.reportedUserId(),
        request.reason(),
        normalizeContent(request.content())
    ));
    return CreateReportResponse.from(report);
  }

  private void validateParticipant(Long partyId, Long userId, ReportErrorCode errorCode) {
    if (!partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
        partyId,
        userId,
        PartyParticipantStatus.JOINED
    )) {
      throw new ReportException(errorCode);
    }
  }

  private void validateReportedUser(Long reporterId, Long partyId, Long reportedUserId) {
    if (reporterId.equals(reportedUserId)) {
      throw new ReportException(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
    }
    validateParticipant(partyId, reportedUserId, ReportErrorCode.REPORTED_USER_NOT_PARTICIPANT);
  }

  private String normalizeContent(String content) {
    return StringUtils.hasText(content) ? content.trim() : null;
  }
}
