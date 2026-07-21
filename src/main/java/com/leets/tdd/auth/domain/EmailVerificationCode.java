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
 * 요청 한도(1시간 5회)도 이 테이블의 최근 1시간 row 개수로 센다.
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

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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
}
