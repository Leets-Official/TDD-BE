package com.leets.tdd.auth.repository;

import com.leets.tdd.auth.domain.EmailPurpose;
import com.leets.tdd.auth.domain.EmailVerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 인증코드/요청횟수 저장소. Redis 대신 MySQL(email_verification_codes 테이블)을 쓴다.
 * - 인증코드 TTL(5분): expiresAt 컬럼과 현재 시각 비교로 흉내
 * - 요청횟수(5분 내 3회 초과 시 차단): 최근 5분 이내 저장된 row 개수로 계산
 *   -> saveCode() 호출 자체가 요청 1건을 기록하는 것이라 별도 카운트 증가 로직이 필요 없다.
 * - 인증코드는 평문으로 저장하지 않고 BCrypt 해시로 저장한다(DB 조회 권한이 노출돼도
 *   유효시간 내 코드를 그대로 재사용하지 못하게).
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationRepository {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration REQUEST_WINDOW = Duration.ofMinutes(5);
    private static final int MAX_REQUEST_COUNT_PER_WINDOW = 3;

    private final EmailVerificationCodeJpaRepository jpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void saveCode(String email, EmailPurpose purpose, String code) {
        LocalDateTime expiresAt = LocalDateTime.now().plus(CODE_TTL);
        String codeHash = passwordEncoder.encode(code);
        jpaRepository.save(new EmailVerificationCode(email, purpose, codeHash, expiresAt));
    }

    /**
     * 인증코드 확인(verify) API용. 만료 여부와 무관하게 가장 최근 발급 기록을 반환한다.
     * 일치/만료 여부는 호출부(Service)에서 각각 구분해서 판단한다.
     */
    public Optional<EmailVerificationCode> findLatest(String email, EmailPurpose purpose) {
        return jpaRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);
    }

    @Transactional
    public void markVerified(EmailVerificationCode verification) {
        verification.markVerified();
        jpaRepository.save(verification);
    }

    /**
     * 인증코드 확인(verify) 실패 시 attempt_count 증가. ERD의 attempt_count 컬럼용.
     */
    @Transactional
    public void increaseAttemptCount(EmailVerificationCode verification) {
        verification.increaseAttemptCount();
        jpaRepository.save(verification);
    }

    @Transactional
    public void deleteCode(String email, EmailPurpose purpose) {
        jpaRepository.deleteByEmailAndPurpose(email, purpose);
    }

    /**
     * "최근(window 이내) 인증됐는지 확인 + 소비(삭제)"를 하나의 원자적 DB 연산으로 처리한다.
     * true를 반환하면 이번 호출이 그 인증 기록을 실제로 소비(삭제)한 것이고,
     * false면 조건을 만족하는 기록이 없었던(=인증 안 됐거나 이미 다른 요청이 먼저 소비한) 것이다.
     */
    @Transactional
    public boolean consumeIfRecentlyVerified(String email, EmailPurpose purpose, Duration window) {
        LocalDateTime cutoff = LocalDateTime.now().minus(window);
        return jpaRepository.deleteVerifiedWithinWindow(email, purpose, cutoff) > 0;
    }

    public boolean isRequestLimitExceeded(String email) {
        LocalDateTime windowStart = LocalDateTime.now().minus(REQUEST_WINDOW);
        return jpaRepository.countByEmailAndCreatedAtAfter(email, windowStart) >= MAX_REQUEST_COUNT_PER_WINDOW;
    }
}
