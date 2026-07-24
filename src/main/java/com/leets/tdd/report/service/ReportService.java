package com.leets.tdd.report.service;

import com.leets.tdd.report.dto.CreateReportRequest;
import com.leets.tdd.report.dto.CreateReportResponse;

public interface ReportService {

  CreateReportResponse createReport(Long reporterId, Long partyId, CreateReportRequest request);
}
