package com.leets.tdd.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leets.tdd.global.jwt.UserPrincipal;
import com.leets.tdd.report.domain.ReportReason;
import com.leets.tdd.report.dto.CreateReportResponse;
import com.leets.tdd.report.service.ReportService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ReportService reportService;

  @Test
  void 신고를_등록한다() throws Exception {
    given(reportService.createReport(any(), any(), any())).willReturn(new CreateReportResponse(
        7L,
        10L,
        2L,
        "NO_SHOW",
        "PENDING",
        LocalDateTime.of(2026, 7, 24, 20, 0)
    ));

    mockMvc.perform(post("/api/v1/parties/10/reports")
            .with(user(1L))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReportRequest(2L, ReportReason.NO_SHOW, null))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.reportId").value(7))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void 기타_사유는_내용이_필요하다() throws Exception {
    mockMvc.perform(post("/api/v1/parties/10/reports")
            .with(user(1L))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReportRequest(2L, ReportReason.ETC, "  "))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  private RequestPostProcessor user(Long userId) {
    return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(userId), null, List.of()));
  }

  private record ReportRequest(Long reportedUserId, ReportReason reason, String content) {
  }
}
