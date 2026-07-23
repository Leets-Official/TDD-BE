package com.leets.tdd.global.health;

import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig의 securityFilterChain 빈이 JwtAuthenticationFilter를 내부에서 직접 만들어
// 체인에 등록하는데, 그 필터가 JwtProvider를 필요로 하므로 슬라이스 테스트에도 목(mock)해준다.
// JwtAuthenticationFilter 자체는 더 이상 별도 빈이 아니라서(이유는 SecurityConfig 주석 참고)
// 여기서 따로 @Import할 필요는 없다. 이 테스트는 토큰을 아예 안 보내는 케이스만 확인하므로
// JwtProvider 동작 자체는 중요하지 않다.
@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    void 헬스체크는_200과_UP을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void 공개되지_않은_엔드포인트는_인증을_요구한다() throws Exception {
        mockMvc.perform(get("/api/v1/private"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증 토큰이 없습니다."));
    }
}
