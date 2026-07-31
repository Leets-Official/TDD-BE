package com.leets.tdd.user.repository;

import com.leets.tdd.user.domain.DormStatus;
import com.leets.tdd.user.domain.Dormitory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DormitoryRepository extends JpaRepository<Dormitory, Long> {

    Optional<Dormitory> findByUserId(Long userId);

    List<Dormitory> findAllByUserIdIn(Collection<Long> userIds);

    // 매일 00:00 만료 배치용: 승인된 상태인데 학기 만료 시각이 지난 건을 찾는다.
    List<Dormitory> findByDormStatusAndDormVerifiedUntilBefore(DormStatus dormStatus, LocalDateTime now);
}
