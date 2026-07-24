package com.leets.tdd.auth.controller;

import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.auth.service.AuthService;
import com.leets.tdd.auth.service.EmailVerificationService;
import com.leets.tdd.global.config.SecurityConfig;
import com.leets.tdd.user.exception.UserErrorCode;
import com.leets.tdd.user.exception.UserException;
import com.leets.tdd.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig가 필터 체인 안에서 JwtProvider/UserRepository를 필요로 하므로 슬라이스 테스트에서도
// 목(mock)해준다. password-reset은 비인증(public) 엔드포인트라 토큰 없이도 호출돼야 한다.
// 또한 AuthService.resetPassword()가 던지는 UserException(INVALID_VERIFICATION/USER_NOT_FOUND)이
// AuthController에서도 500이 아니라 올바른 상태코드/메시지로 응답되는지 검증한다
// (UserExceptionHandler의 basePackages가 auth.controller까지 포함하도록 고친 부분의 회귀 테스트).
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

    private static final String VALID_BODY =
            "{\"email\":\"abcd@gachon.ac.kr\",\"newPassword\":\"new-pw123\"}";

    @Test
    @DisplayName("토큰 없이도 비밀번호 재설정 요청이 처리된다(공개 엔드포인트)")
    void resetPassword_publicEndpoint_success() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/password-reset")
                        .contentType("application/json")
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("비밀번호가 재설정되었습니다."));

        verify(authService).resetPassword(any());
    }

    @Test
    @DisplayName("이메일 인증 기록이 없으면(INVALID_VERIFICATION) 500이 아니라 400으로 응답한다")
    void resetPassword_invalidVerification_returns400NotInternalServerError() throws Exception {
        doThrow(new UserException(UserErrorCode.INVALID_VERIFICATION))
                .when(authService).resetPassword(any());

        mockMvc.perform(patch("/api/v1/auth/password-reset")
                        .contentType("application/json")
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증정보가 유효하지 않습니다. 이메일 인증을 다시 해주세요."));
    }

    @Test
    @DisplayName("해당 이메일의 유저가 없으면(USER_NOT_FOUND) 500이 아니라 404로 응답한다")
    void resetPassword_userNotFound_returns404NotInternalServerError() throws Exception {
        doThrow(new UserException(UserErrorCode.USER_NOT_FOUND))
                .when(authService).resetPassword(any());

        mockMvc.perform(patch("/api/v1/auth/password-reset")
                        .contentType("application/json")
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("학교 이메일 형식이 아니면 400과 함께 필드 에러를 반환한다")
    void resetPassword_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/password-reset")
                        .contentType("application/json")
                        .content("{\"email\":\"not-a-school-email@gmail.com\",\"newPassword\":\"new-pw123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(authService, never()).resetPassword(any());
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400을 반환한다")
    void resetPassword_passwordTooShort_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/password-reset")
                        .contentType("application/json")
                        .content("{\"email\":\"abcd@gachon.ac.kr\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).resetPassword(any());
    }
}
