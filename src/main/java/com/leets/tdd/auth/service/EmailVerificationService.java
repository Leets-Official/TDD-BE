package com.leets.tdd.auth.service;

import com.leets.tdd.auth.domain.EmailPurpose;
import com.leets.tdd.auth.domain.EmailVerificationCode;
import com.leets.tdd.auth.dto.EmailVerificationRequest;
import com.leets.tdd.auth.dto.VerifyEmailCodeRequest;
import com.leets.tdd.auth.exception.AuthErrorCode;
import com.leets.tdd.auth.exception.AuthException;
import com.leets.tdd.auth.repository.EmailVerificationRepository;
import com.leets.tdd.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 이슈 #23 - 학교 이메일 인증 메일 발송 API
 * 시나리오: (SIGNUP인 경우) 가입 여부 검증 -> 요청 횟수 검증 -> 6자리 코드 생성/저장(TTL 5분) -> 메일 발송
 * 이메일 형식(학교 이메일) 검증은 EmailVerificationRequest의 @Pattern에서 처리하고,
 * 실패 시 GlobalExceptionHandler가 fieldErrors로 응답한다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    public void sendVerificationCode(EmailVerificationRequest request) {
        String email = request.email();
        EmailPurpose purpose = request.purpose();

        if (purpose == EmailPurpose.SIGNUP) {
            validateNotAlreadyRegistered(email);
        }

        validateRequestLimit(email);

        String code = generateCode();
        emailVerificationRepository.saveCode(email, purpose, code);

        mailService.sendVerificationCode(email, code);
    }

    /**
     * 인증코드 확인.
     * purpose를 생략하면(회원가입 흐름 명세) SIGNUP으로 간주하고,
     * purpose가 있으면(비밀번호 재설정 흐름 명세) 해당 purpose로 저장된 코드와 비교한다.
     */
    public void verifyCode(VerifyEmailCodeRequest request) {
        String email = request.email();
        EmailPurpose purpose = request.purposeOrDefault();

        EmailVerificationCode verification = emailVerificationRepository.findLatest(email, purpose)
                .orElseThrow(() -> new AuthException(AuthErrorCode.CODE_MISMATCH));

        if (verification.isExpired()) {
            throw new AuthException(AuthErrorCode.CODE_EXPIRED);
        }

        if (!passwordEncoder.matches(request.code(), verification.getCode())) {
            throw new AuthException(AuthErrorCode.CODE_MISMATCH);
        }

        emailVerificationRepository.markVerified(verification);
    }

    private void validateNotAlreadyRegistered(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(AuthErrorCode.ALREADY_REGISTERED_EMAIL);
        }
    }

    private void validateRequestLimit(String email) {
        if (emailVerificationRepository.isRequestLimitExceeded(email)) {
            throw new AuthException(AuthErrorCode.VERIFICATION_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private String generateCode() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%0" + CODE_LENGTH + "d", code);
    }
}
