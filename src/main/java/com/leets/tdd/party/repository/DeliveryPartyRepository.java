package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.settlement.domain.SettlementStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
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

  // 탈퇴 제한: 방장(creator)으로서 진행 중이거나(RECRUITING/CLOSED/ORDERED) 정산이 안 끝난 팟이 있는지 확인
  boolean existsByCreatorIdAndStatusIn(Long creatorId, Collection<PartyStatus> statuses);

  boolean existsByCreatorIdAndStatusAndSettlementStatusNotIn(
          Long creatorId,
          PartyStatus status,
          Collection<SettlementStatus> settlementStatuses
  );

  // 탈퇴 제한: 참여자(participant)로서 진행 중이거나 정산이 안 끝난 팟이 있는지 확인
  boolean existsByIdInAndStatusIn(Collection<Long> ids, Collection<PartyStatus> statuses);

  boolean existsByIdInAndStatusAndSettlementStatusNotIn(
          Collection<Long> ids,
          PartyStatus status,
          Collection<SettlementStatus> settlementStatuses
  );
}
