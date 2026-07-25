package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.settlement.domain.SettlementStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DeliveryPartyRepository extends JpaRepository<DeliveryParty, Long> {

  // 배달팟 목록 조회
  List<DeliveryParty> findAllByOrderByCreatedAtDesc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<DeliveryParty> findWithLockById(Long id);

  List<DeliveryParty> findAllByCreatorIdAndSettlementStatus(
          Long creatorId,
          SettlementStatus settlementStatus
  );
}
