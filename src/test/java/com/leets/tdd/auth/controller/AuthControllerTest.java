package com.leets.tdd.auth.controller;

import com.leets.tdd.auth.dto.LoginResponse;
import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.auth.service.AuthService;
import com.leets.tdd.auth.service.EmailVerificationService;
import com.leets.tdd.global.config.SecurityConfig;
import com.leets.tdd.user.domain.UserStatus;
import com.leets.tdd.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig가 필터 체인 안에서 JwtProvider/UserRepository를 필요로 하므로 슬라이스 테스트에서도
// 목(mock)해준다. 특히 여기서는 "발급된 access token이 서명상 유효해도, DB의 최신 계정 상태가
// DELETED/BANNED면 인증되지 않는다"는 동작을 실제 필터 체인(SecurityConfig)을 통해 검증한다
// (탈퇴 직후 access token 재사용 회귀 방지 - PR #46 리뷰 P1/P2).
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("토큰 없이 로그아웃을 요청하면 401을 반환한다")
    void logout_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증 토큰이 없습니다."));

        verify(authService, never()).logout(any());
    }

    @Test
    @DisplayName("유효한 토큰이고 ACTIVE 계정이면 로그아웃에 성공한다")
    void logout_withActiveAccount_returns200() throws Exception {
        when(jwtProvider.resolveToken("Bearer valid-token")).thenReturn("valid-token");
        when(jwtProvider.parseUserId("valid-token")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(1L);
    }

    @Test
    @DisplayName("탈퇴(DELETED) 직후 예전에 발급된 access token으로는 로그아웃 요청도 인증되지 않는다")
    void logout_withTokenOfDeletedAccount_returns401AndNeverCallsService() throws Exception {
        when(jwtProvider.resolveToken("Bearer stale-token")).thenReturn("stale-token");
        when(jwtProvider.parseUserId("stale-token")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.DELETED));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer stale-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증 토큰이 유효하지 않습니다."));

        verify(authService, never()).logout(any());
    }

    @Test
    @DisplayName("BANNED 계정의 토큰이면 403과 함께 이용 제한 메시지를 반환한다")
    void logout_withTokenOfBannedAccount_returns403() throws Exception {
        when(jwtProvider.resolveToken("Bearer banned-token")).thenReturn("banned-token");
        when(jwtProvider.parseUserId("banned-token")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.BANNED));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer banned-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이용이 제한된 계정입니다."));

        verify(authService, never()).logout(any());
    }

    @Test
    @DisplayName("로그인은 공개 엔드포인트라 토큰 없이도 서비스가 호출된다")
    void login_publicEndpoint_noTokenNeeded() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("access", "refresh", "Bearer"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"abcd@gachon.ac.kr\",\"password\":\"pw123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }
}
