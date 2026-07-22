package com.leets.tdd.auth.service;

import com.leets.tdd.auth.domain.EmailPurpose;
import com.leets.tdd.auth.domain.EmailVerificationCode;
import com.leets.tdd.auth.dto.EmailVerificationRequest;
import com.leets.tdd.auth.dto.VerifyEmailCodeRequest;
import com.leets.tdd.auth.exception.AuthErrorCode;
import com.leets.tdd.auth.exception.AuthException;
import com.leets.tdd.auth.repository.EmailVerificationRepository;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("SIGNUP 목적으로 이미 가입된 이메일이면 예외가 발생한다")
    void alreadyRegisteredEmail() {
        EmailVerificationRequest request = new EmailVerificationRequest("abcd@gachon.ac.kr", EmailPurpose.SIGNUP);
        when(userRepository.existsByEmail("abcd@gachon.ac.kr")).thenReturn(true);

        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.ALREADY_REGISTERED_EMAIL.getMessage());

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("5분 내 요청 횟수(3회)를 초과하면 예외가 발생한다")
    void exceedRequestLimit() {
        EmailVerificationRequest request = new EmailVerificationRequest("abcd@gachon.ac.kr", EmailPurpose.RESET_PASSWORD);
        when(emailVerificationRepository.isRequestLimitExceeded("abcd@gachon.ac.kr")).thenReturn(true);

        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.VERIFICATION_REQUEST_LIMIT_EXCEEDED.getMessage());

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("정상 요청이면 인증코드를 저장하고 메일을 발송한다")
    void sendVerificationCodeSuccess() {
        EmailVerificationRequest request = new EmailVerificationRequest("abcd@gachon.ac.kr", EmailPurpose.RESET_PASSWORD);

        emailVerificationService.sendVerificationCode(request);

        verify(emailVerificationRepository).saveCode(eq("abcd@gachon.ac.kr"), eq(EmailPurpose.RESET_PASSWORD), anyString());
        verify(mailService).sendVerificationCode(eq("abcd@gachon.ac.kr"), anyString());
    }

    @Test
    @DisplayName("발송된 코드가 없으면 확인 시 예외가 발생한다")
    void verifyCode_noCodeRequested() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest("abcd@gachon.ac.kr", "123456", null);
        when(emailVerificationRepository.findLatest("abcd@gachon.ac.kr", EmailPurpose.SIGNUP))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.CODE_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("이미 검증에 성공한 코드면 만료 전이라도 다시 확인 시 예외가 발생한다")
    void verifyCode_alreadyVerified() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest("abcd@gachon.ac.kr", "123456", null);
        EmailVerificationCode alreadyVerified = new EmailVerificationCode(
                "abcd@gachon.ac.kr", EmailPurpose.SIGNUP, "hashed-123456", LocalDateTime.now().plusMinutes(5));
        alreadyVerified.markVerified();
        when(emailVerificationRepository.findLatest("abcd@gachon.ac.kr", EmailPurpose.SIGNUP))
                .thenReturn(Optional.of(alreadyVerified));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.CODE_ALREADY_VERIFIED.getMessage());
    }

    @Test
    @DisplayName("코드가 만료되었으면 확인 시 예외가 발생한다")
    void verifyCode_expired() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest("abcd@gachon.ac.kr", "123456", null);
        EmailVerificationCode expired = new EmailVerificationCode(
                "abcd@gachon.ac.kr", EmailPurpose.SIGNUP, "123456", LocalDateTime.now().minusMinutes(1));
        when(emailVerificationRepository.findLatest("abcd@gachon.ac.kr", EmailPurpose.SIGNUP))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.CODE_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("코드가 일치하지 않으면 확인 시 예외가 발생한다")
    void verifyCode_mismatch() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest("abcd@gachon.ac.kr", "111111", null);
        EmailVerificationCode stored = new EmailVerificationCode(
                "abcd@gachon.ac.kr", EmailPurpose.SIGNUP, "hashed-123456", LocalDateTime.now().plusMinutes(5));
        when(emailVerificationRepository.findLatest("abcd@gachon.ac.kr", EmailPurpose.SIGNUP))
                .thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("111111", "hashed-123456")).thenReturn(false);

        assertThatThrownBy(() -> emailVerificationService.verifyCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.CODE_MISMATCH.getMessage());

        verify(emailVerificationRepository).increaseAttemptCount(stored);
    }

    @Test
    @DisplayName("확인 실패가 3회 누적되면 코드가 만료 전이라도 이후 시도는 거부된다")
    void verifyCode_attemptLimitExceeded() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest("abcd@gachon.ac.kr", "123456", null);
        EmailVerificationCode stored = new EmailVerificationCode(
                "abcd@gachon.ac.kr", EmailPurpose.SIGNUP, "hashed-123456", LocalDateTime.now().plusMinutes(5));
        stored.increaseAttemptCount();
        stored.increaseAttemptCount();
        stored.increaseAttemptCount();
        when(emailVerificationRepository.findLatest("abcd@gachon.ac.kr", EmailPurpose.SIGNUP))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.VERIFICATION_ATTEMPT_LIMIT_EXCEEDED.getMessage());

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("purpose를 생략하면 SIGNUP 코드로 확인한다")
    void verifyCode_defaultsToSignupPurpose() {
        VerifyEmailCodeRequest request = new VerifyEmailCodeRequest("abcd@gachon.ac.kr", "123456", null);
        EmailVerificationCode stored = new EmailVerificationCode(
                "abcd@gachon.ac.kr", EmailPurpose.SIGNUP, "hashed-123456", LocalDateTime.now().plusMinutes(5));
        when(emailVerificationRepository.findLatest("abcd@gachon.ac.kr", EmailPurpose.SIGNUP))
                .thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("123456", "hashed-123456")).thenReturn(true);

        assertThatCode(() -> emailVerificationService.verifyCode(request)).doesNotThrowAnyException();

        verify(emailVerificationRepository).markVerified(stored);
    }

    @Test
    @DisplayName("purpose가 RESET_PASSWORD이면 해당 목적의 코드로 확인한다")
    void verifyCode_resetPasswordPurpose() {
        VerifyEmailCodeRequest request =
                new VerifyEmailCodeRequest("abcd@gachon.ac.kr", "123456", EmailPurpose.RESET_PASSWORD);
        EmailVerificationCode stored = new EmailVerificationCode(
                "abcd@gachon.ac.kr", EmailPurpose.RESET_PASSWORD, "hashed-123456", LocalDateTime.now().plusMinutes(5));
        when(emailVerificationRepository.findLatest("abcd@gachon.ac.kr", EmailPurpose.RESET_PASSWORD))
                .thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("123456", "hashed-123456")).thenReturn(true);

        emailVerificationService.verifyCode(request);

        verify(emailVerificationRepository).markVerified(stored);
    }
}
