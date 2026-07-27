package com.leets.tdd.party.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leets.tdd.global.config.SecurityConfig;
import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.party.dto.response.PartyParticipantListResponse;
import com.leets.tdd.party.dto.response.PartyParticipantResponse;
import com.leets.tdd.party.service.DeliveryPartyService;
import com.leets.tdd.user.domain.UserStatus;
import com.leets.tdd.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryPartyController.class)
@Import(SecurityConfig.class)
class DeliveryPartyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private DeliveryPartyService deliveryPartyService;

    @Test
    void 토큰_없이_참여자_목록을_조회하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/parties/15/participants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

        verify(deliveryPartyService, never()).getPartyParticipants(15L);
    }

    @Test
    void 유효한_토큰으로_참여자_목록을_조회한다() throws Exception {
        when(jwtProvider.resolveToken("Bearer valid-token")).thenReturn("valid-token");
        when(jwtProvider.parseUserId("valid-token")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.ACTIVE));
        when(deliveryPartyService.getPartyParticipants(15L)).thenReturn(
                new PartyParticipantListResponse(
                        15L,
                        List.of(new PartyParticipantResponse(1L, "대교", null, "OWNER"))
                )
        );

        mockMvc.perform(get("/api/v1/parties/15/participants")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("참여자 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.partyId").value(15))
                .andExpect(jsonPath("$.data.participants[0].nickname").value("대교"))
                .andExpect(jsonPath("$.data.participants[0].role").value("OWNER"));

        verify(deliveryPartyService).getPartyParticipants(15L);
    }
}
