package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.settlement.domain.SettlementStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPartyRepository extends JpaRepository<DeliveryParty, Long> {

  List<DeliveryParty> findAllByOrderByCreatedAtDesc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<DeliveryParty> findWithLockById(Long id);

  List<DeliveryParty> findAllByCreatorIdAndSettlementStatus(Long creatorId, SettlementStatus settlementStatus);
}
