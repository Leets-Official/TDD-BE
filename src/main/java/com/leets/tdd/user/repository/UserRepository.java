package com.leets.tdd.user.repository;

import com.leets.tdd.user.domain.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    List<User> findAllByIdIn(Collection<Long> ids);
}
