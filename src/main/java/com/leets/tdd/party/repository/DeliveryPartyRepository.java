package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.settlement.domain.SettlementStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPartyRepository extends JpaRepository<DeliveryParty, Long> {

  List<DeliveryParty> findAllByOrderByCreatedAtDesc();

  List<DeliveryParty> findAllByCreatorIdAndSettlementStatus(Long creatorId, SettlementStatus settlementStatus);
}
