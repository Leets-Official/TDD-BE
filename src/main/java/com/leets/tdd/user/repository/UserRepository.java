package com.leets.tdd.user.repository;

import com.leets.tdd.user.domain.User;
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

    // 만료된 refresh token 해시 정리용(RefreshTokenCleanupScheduler). 이미 비어있는 건 건드리지 않는다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.refreshTokenHash = '' "
            + "WHERE u.refreshTokenExpiresAt < :cutoff AND u.refreshTokenHash <> ''")
    int clearExpiredRefreshTokens(@Param("cutoff") LocalDateTime cutoff);
}