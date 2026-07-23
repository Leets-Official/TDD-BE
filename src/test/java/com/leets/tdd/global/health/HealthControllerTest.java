package com.leets.tdd.global.health;

import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.global.config.JwtAuthenticationFilter;
import com.leets.tdd.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig가 JwtAuthenticationFilter를 filter chain에 등록하고, 그 필터는 JwtProvider가
// 있어야 만들어지므로 슬라이스 테스트에도 같이 임포트/목(mock)해준다. 이 테스트는 토큰을 아예
// 안 보내는 케이스만 확인하므로 JwtProvider 동작 자체는 중요하지 않다.
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
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
                .andExpect(status().isForbidden());
    }
}
