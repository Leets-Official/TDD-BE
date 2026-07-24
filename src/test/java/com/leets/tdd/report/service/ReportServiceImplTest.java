package com.leets.tdd.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.report.domain.Report;
import com.leets.tdd.report.domain.ReportReason;
import com.leets.tdd.report.domain.ReportStatus;
import com.leets.tdd.report.dto.CreateReportRequest;
import com.leets.tdd.report.dto.CreateReportResponse;
import com.leets.tdd.report.exception.ReportErrorCode;
import com.leets.tdd.report.exception.ReportException;
import com.leets.tdd.report.repository.ReportRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

  @Mock
  private DeliveryPartyRepository deliveryPartyRepository;

  @Mock
  private PartyParticipantRepository partyParticipantRepository;

  @Mock
  private ReportRepository reportRepository;

  @InjectMocks
  private ReportServiceImpl reportService;

  @Test
  void 참여자를_신고하면_PENDING_상태로_저장한다() {
    given(deliveryPartyRepository.findWithLockById(10L)).willReturn(Optional.of(party()));
    givenParticipant(10L, 1L, true);
    givenParticipant(10L, 2L, true);
    given(reportRepository.existsByPartyIdAndReporterIdAndReportedUserIdAndReasonAndStatus(
        10L, 1L, 2L, ReportReason.NO_SHOW, ReportStatus.PENDING
    )).willReturn(false);
    given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));

    CreateReportResponse response = reportService.createReport(
        1L,
        10L,
        new CreateReportRequest(2L, ReportReason.NO_SHOW, "  약속 시간에 나타나지 않았습니다.  ")
    );

    ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
    verify(reportRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.PENDING);
    assertThat(captor.getValue().getContent()).isEqualTo("약속 시간에 나타나지 않았습니다.");
    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.reason()).isEqualTo("NO_SHOW");
  }

  @Test
  void 팟_참여자가_아니면_신고할_수_없다() {
    given(deliveryPartyRepository.findWithLockById(10L)).willReturn(Optional.of(party()));
    givenParticipant(10L, 1L, false);

    assertThatThrownBy(() -> reportService.createReport(
        1L,
        10L,
        new CreateReportRequest(2L, ReportReason.NO_SHOW, null)
    )).isInstanceOf(ReportException.class)
        .extracting(exception -> ((ReportException) exception).getErrorCode())
        .isEqualTo(ReportErrorCode.NOT_PARTICIPANT);
  }

  @Test
  void 자기_자신은_신고할_수_없다() {
    given(deliveryPartyRepository.findWithLockById(10L)).willReturn(Optional.of(party()));
    givenParticipant(10L, 1L, true);

    assertThatThrownBy(() -> reportService.createReport(
        1L,
        10L,
        new CreateReportRequest(1L, ReportReason.NO_SHOW, null)
    )).isInstanceOf(ReportException.class)
        .extracting(exception -> ((ReportException) exception).getErrorCode())
        .isEqualTo(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
  }

  @Test
  void 신고_대상이_참여자가_아니면_거절한다() {
    given(deliveryPartyRepository.findWithLockById(10L)).willReturn(Optional.of(party()));
    givenParticipant(10L, 1L, true);
    givenParticipant(10L, 2L, false);

    assertThatThrownBy(() -> reportService.createReport(
        1L,
        10L,
        new CreateReportRequest(2L, ReportReason.NO_SHOW, null)
    )).isInstanceOf(ReportException.class)
        .extracting(exception -> ((ReportException) exception).getErrorCode())
        .isEqualTo(ReportErrorCode.REPORTED_USER_NOT_PARTICIPANT);
  }

  @Test
  void 같은_신고자와_팟과_대상과_사유의_PENDING_신고는_중복될_수_없다() {
    given(deliveryPartyRepository.findWithLockById(10L)).willReturn(Optional.of(party()));
    givenParticipant(10L, 1L, true);
    givenParticipant(10L, 2L, true);
    given(reportRepository.existsByPartyIdAndReporterIdAndReportedUserIdAndReasonAndStatus(
        10L, 1L, 2L, ReportReason.NO_SHOW, ReportStatus.PENDING
    )).willReturn(true);

    assertThatThrownBy(() -> reportService.createReport(
        1L,
        10L,
        new CreateReportRequest(2L, ReportReason.NO_SHOW, null)
    )).isInstanceOf(ReportException.class)
        .extracting(exception -> ((ReportException) exception).getErrorCode())
        .isEqualTo(ReportErrorCode.REPORT_ALREADY_EXISTS);
  }

  private void givenParticipant(Long partyId, Long userId, boolean joined) {
    given(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
        partyId,
        userId,
        PartyParticipantStatus.JOINED
    )).willReturn(joined);
  }

  private DeliveryParty party() {
    return org.mockito.Mockito.mock(DeliveryParty.class);
  }
}
