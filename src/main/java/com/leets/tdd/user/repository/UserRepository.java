package com.leets.tdd.user.repository;

import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);

    List<User> findAllByIdIn(Collection<Long> ids);

    // JwtAuthenticationFilter가 매 요청마다 탈퇴(DELETED)/제한(BANNED) 여부만 확인하는 용도라,
    // User 엔티티 전체를 로딩하지 않고 status 컬럼만 읽어온다.
    @Query("SELECT u.status FROM User u WHERE u.id = :userId")
    Optional<UserStatus> findStatusById(@Param("userId") Long userId);

    // 만료된 refresh token 해시 정리용(RefreshTokenCleanupScheduler). 이미 비어있는 건 건드리지 않는다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.refreshTokenHash = '' "
            + "WHERE u.refreshTokenExpiresAt < :cutoff AND u.refreshTokenHash <> ''")
    int clearExpiredRefreshTokens(@Param("cutoff") LocalDateTime cutoff);

    // 로그인 실패 횟수 증가를 하나의 UPDATE로 원자적으로 처리한다(find-후-메모리증가-save 방식은
    // 동시 요청이 오면 서로의 증가분을 덮어써서 3회 제한이 우회될 수 있어서 이렇게 바꿨다).
    // windowStart(=now - 5분)보다 마지막 실패시각이 이전이면(또는 아예 없으면) 1로 리셋,
    // 아니면 기존 값에서 +1 한다. DB가 이 UPDATE 실행 동안 행 잠금을 잡아서 동시 요청도 순차 처리된다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET "
            + "u.failedLoginAttempts = CASE WHEN u.lastFailedLoginAt IS NULL OR u.lastFailedLoginAt < :windowStart "
            + "THEN 1 ELSE u.failedLoginAttempts + 1 END, "
            + "u.lastFailedLoginAt = :now "
            + "WHERE u.id = :userId")
    void recordFailedLogin(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("windowStart") LocalDateTime windowStart
    );
}
