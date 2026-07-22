package com.leets.tdd.auth.repository;

import com.leets.tdd.auth.domain.EmailPurpose;
import com.leets.tdd.auth.domain.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationCodeJpaRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, EmailPurpose purpose);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    void deleteByEmailAndPurpose(String email, EmailPurpose purpose);

    /**
     * "인증됐는지 확인 + 소비(삭제)"를 하나의 원자적 DELETE로 묶는다.
     * 조건(해당 purpose로 verifiedAt이 cutoff 이후)을 만족하는 row가 있을 때만 삭제하고,
     * 삭제된 row 수를 반환한다. 동시에 같은 email로 여러 요청이 와도 DB가 delete를
     * 직렬화하므로 단 하나의 호출만 1을 받고 나머지는 0을 받는다(TOCTOU 방지).
     */
    @Modifying
    @Query("delete from EmailVerificationCode e "
            + "where e.email = :email and e.purpose = :purpose "
            + "and e.verifiedAt is not null and e.verifiedAt > :cutoff")
    int deleteVerifiedWithinWindow(
            @Param("email") String email,
            @Param("purpose") EmailPurpose purpose,
            @Param("cutoff") LocalDateTime cutoff);
}
