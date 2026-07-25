package com.leets.tdd.auth.service;

import com.leets.tdd.auth.dto.LoginRequest;
import com.leets.tdd.auth.dto.LoginResponse;
import com.leets.tdd.auth.dto.RefreshTokenRequest;
import com.leets.tdd.auth.dto.ResetPasswordRequest;
import com.leets.tdd.auth.exception.AuthErrorCode;
import com.leets.tdd.auth.exception.AuthException;
import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.auth.jwt.RefreshTokenHasher;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.exception.UserErrorCode;
import com.leets.tdd.user.exception.UserException;
import com.leets.tdd.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    private User activeUser() {
        return new User("abcd@gachon.ac.kr", "가나디", "encoded-pw",
                "old-refresh-hash", LocalDateTime.now().plusDays(30));
    }

    private LoginRequest loginRequest() {
        return new LoginRequest("abcd@gachon.ac.kr", "raw-pw");
    }

    private ResetPasswordRequest resetRequest() {
        return new ResetPasswordRequest("abcd@gachon.ac.kr", "new-raw-pw");
    }

    // ===== login =====

    @Test
    @DisplayName("이메일/비밀번호가 맞으면 로그인에 성공하고 토큰을 발급한다")
    void login_success() {
        User user = activeUser();
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(true);
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProvider.getRefreshTokenValidity()).thenReturn(Duration.ofDays(30));
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("new-refresh-hash");

        LoginResponse response = authService.login(loginRequest());

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 LOGIN_FAILED로 응답한다(계정 존재 여부 비노출)")
    void login_userNotFound_throwsLoginFailed() {
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.LOGIN_FAILED.getMessage());
    }

    @Test
    @DisplayName("탈퇴(DELETED)한 계정이면 LOGIN_FAILED로 응답한다(계정 존재 여부 비노출)")
    void login_deletedUser_throwsLoginFailed() {
        User deleted = activeUser();
        deleted.softDelete();
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.LOGIN_FAILED.getMessage());
    }

    @Test
    @DisplayName("제한(BANNED)된 계정이면 ACCOUNT_BANNED로 응답한다")
    void login_bannedUser_throwsAccountBanned() {
        User banned = activeUser();
        banned.ban();
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.ACCOUNT_BANNED.getMessage());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 실패 횟수를 기록하고 LOGIN_FAILED로 응답한다")
    void login_wrongPassword_recordsFailureAndThrowsLoginFailed() {
        User user = activeUser();
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.LOGIN_FAILED.getMessage());

        verify(userRepository).recordFailedLogin(any(), any(), any());
    }

    @Test
    @DisplayName("5분 내 3회 이상 실패해서 잠긴 계정이면 비밀번호가 맞아도 LOGIN_ATTEMPT_LIMIT_EXCEEDED로 응답한다")
    void login_blockedAccount_throwsLoginAttemptLimitExceeded() {
        User user = activeUser();
        ReflectionTestUtils.setField(user, "failedLoginAttempts", 3);
        ReflectionTestUtils.setField(user, "lastFailedLoginAt", LocalDateTime.now());
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED.getMessage());

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("차단 기간(15분)이 지난 계정이면 실패 기록이 남아있어도 정상 로그인된다")
    void login_blockDurationExpired_loginSucceeds() {
        User user = activeUser();
        ReflectionTestUtils.setField(user, "failedLoginAttempts", 3);
        ReflectionTestUtils.setField(user, "lastFailedLoginAt", LocalDateTime.now().minusMinutes(16));
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(true);
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProvider.getRefreshTokenValidity()).thenReturn(Duration.ofDays(30));
        when(refreshTokenHasher.hash(anyString())).thenReturn("hash");

        LoginResponse response = authService.login(loginRequest());

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    // ===== reissueToken =====

    @Test
    @DisplayName("유효한 refresh token이면 access/refresh 토큰을 재발급한다")
    void reissueToken_success() {
        User user = activeUser();
        user.updateRefreshToken("matching-hash", LocalDateTime.now().plusDays(29));
        when(jwtProvider.parseRefreshUserId("old-refresh")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenHasher.hash("old-refresh")).thenReturn("matching-hash");
        when(jwtProvider.createAccessToken(any())).thenReturn("new-access");
        when(jwtProvider.createRefreshToken(any())).thenReturn("new-refresh");
        when(jwtProvider.getRefreshTokenValidity()).thenReturn(Duration.ofDays(30));
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("new-hash");

        LoginResponse response = authService.reissueToken(new RefreshTokenRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("만료된 refresh token이면 REFRESH_TOKEN_EXPIRED로 응답한다")
    void reissueToken_expiredJwt_throwsRefreshTokenExpired() {
        when(jwtProvider.parseRefreshUserId("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));

        assertThatThrownBy(() -> authService.reissueToken(new RefreshTokenRequest("expired")))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.REFRESH_TOKEN_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("서명이 잘못된 refresh token이면 REFRESH_TOKEN_INVALID로 응답한다")
    void reissueToken_malformedJwt_throwsRefreshTokenInvalid() {
        when(jwtProvider.parseRefreshUserId("bad")).thenThrow(new JwtException("bad signature"));

        assertThatThrownBy(() -> authService.reissueToken(new RefreshTokenRequest("bad")))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    @Test
    @DisplayName("토큰은 유효하지만 유저를 찾을 수 없으면 REFRESH_TOKEN_INVALID로 응답한다")
    void reissueToken_userNotFound_throwsRefreshTokenInvalid() {
        when(jwtProvider.parseRefreshUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissueToken(new RefreshTokenRequest("token")))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    @Test
    @DisplayName("탈퇴(DELETED)한 계정이면 REFRESH_TOKEN_INVALID로 응답한다")
    void reissueToken_deletedUser_throwsRefreshTokenInvalid() {
        User deleted = activeUser();
        deleted.softDelete();
        when(jwtProvider.parseRefreshUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> authService.reissueToken(new RefreshTokenRequest("token")))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    @Test
    @DisplayName("제한(BANNED)된 계정이면 ACCOUNT_BANNED로 응답한다")
    void reissueToken_bannedUser_throwsAccountBanned() {
        User banned = activeUser();
        banned.ban();
        when(jwtProvider.parseRefreshUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> authService.reissueToken(new RefreshTokenRequest("token")))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.ACCOUNT_BANNED.getMessage());
    }

    @Test
    @DisplayName("DB에 저장된 해시와 다르면(이미 rotate된 옛 토큰) REFRESH_TOKEN_INVALID로 응답한다")
    void reissueToken_hashMismatch_throwsRefreshTokenInvalid() {
        User user = activeUser();
        user.updateRefreshToken("current-hash", LocalDateTime.now().plusDays(29));
        when(jwtProvider.parseRefreshUserId("old-token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenHasher.hash("old-token")).thenReturn("stale-hash");

        assertThatThrownBy(() -> authService.reissueToken(new RefreshTokenRequest("old-token")))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.REFRESH_TOKEN_INVALID.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("DB상 refresh token 만료 시각이 지났으면 REFRESH_TOKEN_EXPIRED로 응답한다")
    void reissueToken_dbExpiresAtPassed_throwsRefreshTokenExpired() {
        User user = activeUser();
        user.updateRefreshToken("matching-hash", LocalDateTime.now().minusMinutes(1));
        when(jwtProvider.parseRefreshUserId("token")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(refreshTokenHasher.hash("token")).thenReturn("matching-hash");

        assertThatThrownBy(() -> authService.reissueToken(new RefreshTokenRequest("token")))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.REFRESH_TOKEN_EXPIRED.getMessage());
    }

    // ===== logout =====

    @Test
    @DisplayName("로그아웃하면 refresh token 해시가 비워진다")
    void logout_success_clearsRefreshToken() {
        User user = activeUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.logout(1L);

        assertThat(user.getRefreshTokenHash()).isEmpty();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("유저를 찾을 수 없으면 USER_NOT_FOUND로 응답한다")
    void logout_userNotFound_throwsUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(1L))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    // ===== resetPassword =====

    @Test
    @DisplayName("15분 이내 RESET_PASSWORD 인증 기록이 있으면 비밀번호를 재설정한다")
    void resetPassword_success() {
        User user = activeUser();
        when(emailVerificationService.consumePasswordResetVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-raw-pw")).thenReturn("new-encoded-pw");

        authService.resetPassword(resetRequest());

        assertThat(user.getPassword()).isEqualTo("new-encoded-pw");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("RESET_PASSWORD 인증 기록이 없거나(만료/미인증/이미 소비) 15분이 지났으면 INVALID_VERIFICATION 예외가 발생한다")
    void resetPassword_notVerified_throwsInvalidVerification() {
        when(emailVerificationService.consumePasswordResetVerification("abcd@gachon.ac.kr")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPassword(resetRequest()))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.INVALID_VERIFICATION.getMessage());

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("인증은 통과했지만 해당 이메일의 유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
    void resetPassword_userNotFound_throwsUserNotFound() {
        when(emailVerificationService.consumePasswordResetVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(resetRequest()))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("새 비밀번호는 평문이 아니라 인코딩된 값으로 저장된다")
    void resetPassword_storesEncodedPasswordNotRawPassword() {
        User user = activeUser();
        when(emailVerificationService.consumePasswordResetVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-raw-pw")).thenReturn("new-encoded-pw");

        authService.resetPassword(resetRequest());

        assertThat(user.getPassword()).isNotEqualTo("new-raw-pw");
        assertThat(user.getPassword()).isEqualTo("new-encoded-pw");
    }
}