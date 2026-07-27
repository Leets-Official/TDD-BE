package com.leets.tdd.party.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leets.tdd.global.jwt.UserPrincipal;
import com.leets.tdd.party.dto.response.CompleteDeliveryPartyResponse;
import com.leets.tdd.party.service.DeliveryPartyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryPartyController.class)
class DeliveryPartyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryPartyService deliveryPartyService;

    @Test
    void 배달_완료_응답을_반환한다() throws Exception {
        given(deliveryPartyService.completeDelivery(eq(15L), eq(1L)))
                .willReturn(new CompleteDeliveryPartyResponse(15L, "COMPLETED"));

        mockMvc.perform(patch("/api/v1/parties/15/complete")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(1L), null, List.of())))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("배달이 완료되었습니다."))
                .andExpect(jsonPath("$.data.partyId").value(15))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
