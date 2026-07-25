package com.leets.tdd.user.controller;

import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.global.config.SecurityConfig;
import com.leets.tdd.user.domain.UserStatus;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.exception.UserErrorCode;
import com.leets.tdd.user.exception.UserException;
import com.leets.tdd.user.repository.UserRepository;
import com.leets.tdd.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig가 필터 체인 안에서 JwtProvider/UserRepository를 필요로 하므로 슬라이스 테스트에서도
// 목(mock)해준다. 여기서는 특히 "탈퇴(DELETE /me) 직후 같은 access token을 재사용해도 더 이상
// 마이페이지/탈퇴 같은 보호 API를 호출할 수 없다"는 동작을 실제 필터 체인(SecurityConfig)을 통해
// 검증한다(PR #46 리뷰 P1/P2 - 회귀 방지).
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    private void stubValidToken(String token, Long userId, UserStatus status) {
        when(jwtProvider.resolveToken("Bearer " + token)).thenReturn(token);
        when(jwtProvider.parseUserId(token)).thenReturn(userId);
        when(userRepository.findStatusById(userId)).thenReturn(Optional.of(status));
    }

    @Test
    @DisplayName("토큰 없이 마이페이지를 요청하면 401을 반환한다")
    void getMyPage_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증 토큰이 없습니다."));
    }

    @Test
    @DisplayName("ACTIVE 계정의 유효한 토큰이면 마이페이지 조회에 성공한다")
    void getMyPage_activeAccount_returns200() throws Exception {
        stubValidToken("valid-token", 1L, UserStatus.ACTIVE);
        when(userService.getMyPage(1L)).thenReturn(new MyPageResponse(
                "가나디", null, BigDecimal.valueOf(36.5), 0, null, "ACTIVE", null, null, null, null, null));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("가나디"));
    }

    @Test
    @DisplayName("탈퇴(DELETED) 처리된 계정의 토큰으로는 탈퇴 직후에도 마이페이지를 조회할 수 없다")
    void getMyPage_afterWithdrawal_tokenRejected() throws Exception {
        stubValidToken("stale-token", 1L, UserStatus.DELETED);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer stale-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증 토큰이 유효하지 않습니다."));

        verify(userService, never()).getMyPage(any());
    }

    @Test
    @DisplayName("BANNED 계정의 토큰이면 403을 반환한다")
    void getMyPage_bannedAccount_returns403() throws Exception {
        stubValidToken("banned-token", 1L, UserStatus.BANNED);

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer banned-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("이용이 제한된 계정입니다."));

        verify(userService, never()).getMyPage(any());
    }

    @Test
    @DisplayName("비밀번호가 맞으면 탈퇴에 성공한다")
    void withdraw_success_returns200() throws Exception {
        stubValidToken("valid-token", 1L, UserStatus.ACTIVE);

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer valid-token")
                        .contentType("application/json")
                        .content("{\"password\":\"raw-pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).withdraw(eq(1L), any());
    }

    @Test
    @DisplayName("토큰 없이 탈퇴를 요청하면 401을 반환하고 서비스가 호출되지 않는다")
    void withdraw_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .contentType("application/json")
                        .content("{\"password\":\"raw-pw\"}"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).withdraw(any(), any());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 탈퇴가 거부된다")
    void withdraw_passwordMismatch_returns400() throws Exception {
        stubValidToken("valid-token", 1L, UserStatus.ACTIVE);
        doThrow(new UserException(UserErrorCode.PASSWORD_MISMATCH))
                .when(userService).withdraw(eq(1L), any());

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer valid-token")
                        .contentType("application/json")
                        .content("{\"password\":\"wrong-pw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("이미 탈퇴(DELETED)한 계정의 토큰으로는 탈퇴 요청 자체가 인증되지 않는다")
    void withdraw_alreadyDeletedAccountToken_returns401() throws Exception {
        stubValidToken("stale-token", 1L, UserStatus.DELETED);

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer stale-token")
                        .contentType("application/json")
                        .content("{\"password\":\"raw-pw\"}"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).withdraw(any(), any());
    }
}
