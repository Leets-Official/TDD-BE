package com.leets.tdd.report.controller;

import com.leets.tdd.global.jwt.UserPrincipal;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.report.dto.CreateReportRequest;
import com.leets.tdd.report.dto.CreateReportResponse;
import com.leets.tdd.report.exception.ReportErrorCode;
import com.leets.tdd.report.exception.ReportException;
import com.leets.tdd.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Report", description = "배달팟 신고 API")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

  private final ReportService reportService;

  @PostMapping("/parties/{partyId}/reports")
  @Operation(summary = "신고 등록", description = "배달팟 참여자가 같은 팟의 참여자를 신고합니다.")
  public ResponseEntity<ApiResponse<CreateReportResponse>> createReport(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId,
      @Valid @RequestBody CreateReportRequest request
  ) {
    CreateReportResponse response = reportService.createReport(currentUserId(userPrincipal), partyId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("신고가 접수되었습니다.", response));
  }

  private Long currentUserId(UserPrincipal userPrincipal) {
    if (userPrincipal == null || userPrincipal.userId() == null) {
      throw new ReportException(ReportErrorCode.UNAUTHORIZED);
    }
    return userPrincipal.userId();
  }
}
