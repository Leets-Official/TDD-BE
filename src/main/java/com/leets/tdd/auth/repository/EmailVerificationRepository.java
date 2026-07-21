package com.leets.tdd.auth.repository;

import com.leets.tdd.auth.domain.EmailPurpose;
import com.leets.tdd.auth.domain.EmailVerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 인증코드/요청횟수 저장소. Redis 대신 MySQL(email_verification_codes 테이블)을 쓴다.
 * - 인증코드 TTL(5분): expiresAt 컬럼과 현재 시각 비교로 흉내
 * - 요청횟수(1시간 5회): 최근 1시간 이내 저장된 row 개수로 계산
 *   -> saveCode() 호출 자체가 요청 1건을 기록하는 것이라 별도 카운트 증가 로직이 필요 없다.
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationRepository {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final int MAX_REQUEST_COUNT_PER_HOUR = 5;

    private final EmailVerificationCodeJpaRepository jpaRepository;

    @Transactional
    public void saveCode(String email, EmailPurpose purpose, String code) {
        LocalDateTime expiresAt = LocalDateTime.now().plus(CODE_TTL);
        jpaRepository.save(new EmailVerificationCode(email, purpose, code, expiresAt));
    }

    public Optional<String> findCode(String email, EmailPurpose purpose) {
        return jpaRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .filter(verification -> !verification.isExpired())
                .map(EmailVerificationCode::getCode);
    }

    @Transactional
    public void deleteCode(String email, EmailPurpose purpose) {
        jpaRepository.deleteByEmailAndPurpose(email, purpose);
    }

    public boolean isRequestLimitExceeded(String email) {
        LocalDateTime windowStart = LocalDateTime.now().minus(REQUEST_WINDOW);
        return jpaRepository.countByEmailAndCreatedAtAfter(email, windowStart) >= MAX_REQUEST_COUNT_PER_HOUR;
    }
}
