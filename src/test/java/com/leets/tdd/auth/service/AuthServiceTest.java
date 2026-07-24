package com.leets.tdd.auth.service;

import com.leets.tdd.auth.dto.ResetPasswordRequest;
import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.global.jwt.RefreshTokenHasher;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.exception.UserErrorCode;
import com.leets.tdd.user.exception.UserException;
import com.leets.tdd.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 이 PR(feature/password-reset)에서 추가한 AuthService.resetPassword()만 다룬다.
// login/reissueToken/logout은 다른 PR(feature/user-logout-withdrawal) 소관이라 여기서는
// 중복 테스트하지 않는다.
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

    private User existingUser() {
        return new User("abcd@gachon.ac.kr", "가나디", "old-encoded-pw",
                "refresh-hash", LocalDateTime.now().plusDays(30));
    }

    private ResetPasswordRequest resetRequest() {
        return new ResetPasswordRequest("abcd@gachon.ac.kr", "new-raw-pw");
    }

    @Test
    @DisplayName("15분 이내 RESET_PASSWORD 인증 기록이 있으면 비밀번호를 재설정한다")
    void resetPassword_success() {
        User user = existingUser();
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
        User user = existingUser();
        when(emailVerificationService.consumePasswordResetVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-raw-pw")).thenReturn("new-encoded-pw");

        authService.resetPassword(resetRequest());

        assertThat(user.getPassword()).isNotEqualTo("new-raw-pw");
        assertThat(user.getPassword()).isEqualTo("new-encoded-pw");
    }
}
