package com.leets.tdd.settlement.repository;

import com.leets.tdd.settlement.domain.BankAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

  Optional<BankAccount> findByUserId(Long userId);
}
