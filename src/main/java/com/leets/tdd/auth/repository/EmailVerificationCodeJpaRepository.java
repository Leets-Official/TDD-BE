package com.leets.tdd.auth.repository;

import com.leets.tdd.auth.domain.EmailPurpose;
import com.leets.tdd.auth.domain.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationCodeJpaRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, EmailPurpose purpose);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    void deleteByEmailAndPurpose(String email, EmailPurpose purpose);
}
