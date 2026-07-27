package com.leets.tdd.user.controller;

import com.leets.tdd.global.config.SecurityConfig;
import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 인증 없이 열려있는 엔드포인트라는 걸 실제 필터 체인(SecurityConfig)까지 통해서 검증한다
// (토큰 헤더를 아예 안 보내도 401이 아니라 200이 나와야 한다).
@WebMvcTest(InternalToolController.class)
@Import(SecurityConfig.class)
class InternalToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("토큰 없이도 학기 만료일 계산 결과를 받는다(3~8월 인증)")
    void calcDormSemesterEnd_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/internal/dorm-semester-end").param("date", "2026-07-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dorm_verified_until").value("2026-08-31T23:59:59"));
    }

    @Test
    @DisplayName("9~12월 인증이면 다음 해 2월 마지막날로 계산한다")
    void calcDormSemesterEnd_septemberToDecember_returnsNextYearFebruary() throws Exception {
        mockMvc.perform(get("/api/v1/internal/dorm-semester-end").param("date", "2026-09-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dorm_verified_until").value("2027-02-28T23:59:59"));
    }

    @Test
    @DisplayName("date를 생략하면 오늘 날짜 기준으로 계산해서 200을 반환한다")
    void calcDormSemesterEnd_withoutDateParam_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/internal/dorm-semester-end"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dorm_verified_until").exists());
    }
}
