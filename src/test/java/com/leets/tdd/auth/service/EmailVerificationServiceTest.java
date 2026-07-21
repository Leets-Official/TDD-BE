package com.leets.tdd.auth.service;

import com.leets.tdd.auth.domain.EmailPurpose;
import com.leets.tdd.auth.dto.EmailVerificationRequest;
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

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("학교 이메일 형식이 아니면 예외가 발생한다")
    void invalidSchoolEmail() {
        EmailVerificationRequest request = new EmailVerificationRequest("abcd@gmail.com", EmailPurpose.SIGNUP);

        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(request))
                .isInstanceOf(AuthException.class)
                .hasMessage(AuthErrorCode.INVALID_SCHOOL_EMAIL.getMessage());

        verifyNoInteractions(mailService);
    }

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
    @DisplayName("1시간 내 요청 횟수(5회)를 초과하면 예외가 발생한다")
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
}
