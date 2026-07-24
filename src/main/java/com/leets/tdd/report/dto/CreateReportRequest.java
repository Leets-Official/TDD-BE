package com.leets.tdd.report.dto;

import com.leets.tdd.report.domain.ReportReason;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record CreateReportRequest(
    @NotNull @Positive Long reportedUserId,
    @NotNull ReportReason reason,
    @Size(max = 1000) String content
) {

  @AssertTrue(message = "기타 사유를 선택한 경우 신고 내용을 입력해주세요.")
  public boolean isContentValid() {
    return reason != ReportReason.ETC || StringUtils.hasText(content);
  }
}
