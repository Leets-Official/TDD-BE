package com.leets.tdd.party.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leets.tdd.global.config.SecurityConfig;
import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.party.service.DeliveryPartyService;
import com.leets.tdd.user.domain.UserStatus;
import com.leets.tdd.user.repository.UserRepository;
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
    void 토큰_없이_배달팟_삭제를_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/parties/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

        verify(deliveryPartyService, never()).deleteDeliveryParty(1L, 1L);
    }

    @Test
    void 유효한_토큰으로_배달팟을_삭제한다() throws Exception {
        when(jwtProvider.resolveToken("Bearer valid-token")).thenReturn("valid-token");
        when(jwtProvider.parseUserId("valid-token")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.ACTIVE));
        when(deliveryPartyService.deleteDeliveryParty(1L, 1L)).thenReturn(1L);

        mockMvc.perform(delete("/api/v1/parties/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));

        verify(deliveryPartyService).deleteDeliveryParty(1L, 1L);
    }
}
