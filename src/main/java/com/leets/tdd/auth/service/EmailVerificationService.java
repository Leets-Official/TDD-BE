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
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * 이메일 인증 완료(verifiedAt) 후 회원가입(프로필 등록)을 마쳐야 하는 제한 시간.
     * user 도메인의 /users/me(프로필 등록)에서 이 시간 내에 완료됐는지 확인하는 데 쓴다.
     */
    private static final Duration SIGNUP_COMPLETION_WINDOW = Duration.ofMinutes(15);

    /**
     * 이메일 단위로 "요청 횟수 확인 + 코드 저장"을 하나의 임계 구역으로 묶기 위한 락.
     * 동시에 같은 이메일로 여러 요청이 들어와도 한 번에 하나씩만 횟수를 확인하고 저장하게 해서
     * 3회 제한이 동시 요청 사이에서 새는 것을 막는다.
     * Redis 없이 단일 인스턴스로 운영한다는 전제의 임시 방편이라, 인스턴스를 여러 대로
     * 늘리게 되면 분산 락(Redis 등)으로 교체해야 한다.
     */
    private final ConcurrentHashMap<String, Object> emailLocks = new ConcurrentHashMap<>();

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void sendVerificationCode(EmailVerificationRequest request) {
        String email = request.email();
        EmailPurpose purpose = request.purpose();

        if (purpose == EmailPurpose.SIGNUP) {
            validateNotAlreadyRegistered(email);
        }

        Object lock = emailLocks.computeIfAbsent(email, key -> new Object());
        synchronized (lock) {
            validateRequestLimit(email);

            String code = generateCode();
            emailVerificationRepository.saveCode(email, purpose, code);

            // 메일 발송이 실패하면(AuthException) @Transactional에 의해 saveCode()도 함께
            // 롤백된다. 그래서 "코드는 저장됐는데 못 받았다" + 요청 횟수만 소비되는 상황을 막는다.
            mailService.sendVerificationCode(email, code);
        }
    }

    /**
     * 인증코드 확인.
     * purpose를 생략하면(회원가입 흐름 명세) SIGNUP으로 간주하고,
     * purpose가 있으면(비밀번호 재설정 흐름 명세) 해당 purpose로 저장된 코드와 비교한다.
     * 이미 검증에 성공한 코드(verifiedAt != null)는 만료 전이라도 재사용할 수 없다.
     * 코드 불일치가 누적돼 attempt_count가 3회에 도달하면, 코드가 만료되기 전이라도
     * 이후 시도는 모두 거부한다(429).
     */
    public void verifyCode(VerifyEmailCodeRequest request) {
        String email = request.email();
        EmailPurpose purpose = request.purposeOrDefault();

        EmailVerificationCode verification = emailVerificationRepository.findLatest(email, purpose)
                .orElseThrow(() -> new AuthException(AuthErrorCode.CODE_MISMATCH));

        if (verification.getVerifiedAt() != null) {
            throw new AuthException(AuthErrorCode.CODE_ALREADY_VERIFIED);
        }

        if (verification.isExpired()) {
            throw new AuthException(AuthErrorCode.CODE_EXPIRED);
        }

        if (verification.isAttemptLimitExceeded()) {
            throw new AuthException(AuthErrorCode.VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);
        }

        if (!passwordEncoder.matches(request.code(), verification.getCode())) {
            emailVerificationRepository.increaseAttemptCount(verification);
            throw new AuthException(AuthErrorCode.CODE_MISMATCH);
        }

        emailVerificationRepository.markVerified(verification);
    }

    /**
     * user 도메인(/users/me)에서 회원가입을 마무리하기 전에 호출한다.
     * SIGNUP 목적으로 인증에 성공(verifiedAt != null)했고, 그 시각으로부터 15분이 지나지
     * 않았으면 true. signup_token 같은 별도 토큰 없이, 이 DB 조회만으로 신원을 확인한다.
     */
    public boolean isRecentlyVerifiedForSignup(String email) {
        return emailVerificationRepository.findLatest(email, EmailPurpose.SIGNUP)
                .filter(v -> v.getVerifiedAt() != null)
                .filter(v -> LocalDateTime.now().isBefore(v.getVerifiedAt().plus(SIGNUP_COMPLETION_WINDOW)))
                .isPresent();
    }

    /**
     * 회원가입이 완료되면 재사용(replay)을 막기 위해 인증 기록을 지운다.
     */
    @Transactional
    public void consumeSignupVerification(String email) {
        emailVerificationRepository.deleteCode(email, EmailPurpose.SIGNUP);
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
