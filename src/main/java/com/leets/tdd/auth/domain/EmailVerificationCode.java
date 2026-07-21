package com.leets.tdd.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이메일 인증코드 발송 기록.
 * Redis 없이 MySQL로 처리하기 위해, TTL은 별도 만료 기능이 아니라
 * expiresAt 컬럼과 현재 시각을 비교해서 흉내낸다.
 * 요청 한도(5분 3회)도 이 테이블의 최근 5분 row 개수로 센다.
 */
@Entity
@Getter
@Table(name = "email_verification_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailPurpose purpose;

    /**
     * 인증코드의 BCrypt 해시값. 평문 코드는 저장하지 않는다(DB 조회 권한 노출 시
     * 유효시간 내 코드를 그대로 재사용할 수 있는 문제 방지). 비교는 PasswordEncoder.matches()로 한다.
     */
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 인증코드 검증에 성공한 시각. ERD의 verified_time(최대 10분)에 대응.
     * null이면 아직 검증되지 않은 코드.
     */
    @Column
    private LocalDateTime verifiedAt;

    public EmailVerificationCode(String email, EmailPurpose purpose, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.purpose = purpose;
        this.code = code;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markVerified() {
        this.verifiedAt = LocalDateTime.now();
    }
}
