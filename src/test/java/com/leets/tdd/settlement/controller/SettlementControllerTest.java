package com.leets.tdd.settlement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leets.tdd.global.auth.UserPrincipal;
import com.leets.tdd.settlement.dto.response.MySettlementListResponse;
import com.leets.tdd.settlement.dto.response.MySettlementSummaryResponse;
import com.leets.tdd.settlement.dto.response.SettlementDetailResponse;
import com.leets.tdd.settlement.exception.SettlementErrorCode;
import com.leets.tdd.settlement.exception.SettlementException;
import com.leets.tdd.settlement.service.SettlementService;
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

@WebMvcTest(SettlementController.class)
class SettlementControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SettlementService settlementService;

  @Test
  void 정산_요청을_생성한다() throws Exception {
    given(settlementService.createSettlement(any(), any(), any())).willReturn(new SettlementDetailResponse(
        10L,
        "REQUESTED",
        20_000,
        8_000,
        LocalDateTime.of(2026, 7, 24, 12, 0),
        null,
        null,
        List.of()
    ));

    mockMvc.perform(post("/api/v1/parties/10/settlement")
            .with(user(1L))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"totalAmount\":20000,\"payments\":[{\"userId\":2,\"amount\":12000}]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.settlementStatus").value("REQUESTED"));
  }

  @Test
  void 내_정산_현황을_조회한다() throws Exception {
    given(settlementService.getMySettlements(1L)).willReturn(new MySettlementListResponse(
        new MySettlementSummaryResponse(0, 0),
        List.of(),
        List.of()
    ));

    mockMvc.perform(get("/api/v1/users/me/settlements").with(user(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.summary.pendingCount").value(0));
  }

  @Test
  void 정산_도중_중복_요청은_409를_반환한다() throws Exception {
    willThrow(new SettlementException(SettlementErrorCode.SETTLEMENT_ALREADY_REQUESTED))
        .given(settlementService)
        .createSettlement(any(), any(), any());

    mockMvc.perform(post("/api/v1/parties/10/settlement")
            .with(user(1L))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"totalAmount\":20000,\"payments\":[{\"userId\":2,\"amount\":12000}]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false));
  }

  private RequestPostProcessor user(Long userId) {
    return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(userId), null, List.of()));
  }
}
