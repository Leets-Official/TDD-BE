package com.leets.tdd.user.repository;

import com.leets.tdd.user.domain.Dormitory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DormitoryRepository extends JpaRepository<Dormitory, Long> {

    Optional<Dormitory> findByUserId(Long userId);

    List<Dormitory> findAllByUserIdIn(Collection<Long> userIds);
}
