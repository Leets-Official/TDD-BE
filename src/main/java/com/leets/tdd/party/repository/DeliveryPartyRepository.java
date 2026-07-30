package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.projection.RecruitingDeliveryPartyProjection;
import com.leets.tdd.settlement.domain.SettlementStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryPartyRepository extends JpaRepository<DeliveryParty, Long> {

  // 배달팟 목록 조회
  List<DeliveryParty> findAllByOrderByCreatedAtDesc();

  List<DeliveryParty> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);

  @Query(value = """
      SELECT dp.id AS partyId,
             dp.title AS title,
             fc.name AS category,
             (SELECT COUNT(*) FROM party_participants current_pp
               WHERE current_pp.party_id = dp.id AND current_pp.status = 'JOINED') AS currentParticipants,
             dp.min_participants AS minParticipants,
             dp.max_participants AS maxParticipants,
             dp.status AS status,
             dp.order_expected_at AS orderExpectedAt,
             dorm.dormitory AS dormitory
        FROM delivery_parties dp
        JOIN food_categories fc ON fc.id = dp.food_category_id
        LEFT JOIN dormitory dorm ON dorm.user_id = dp.creator_id
       WHERE dp.status = 'RECRUITING'
         AND (:categoryId IS NULL OR dp.food_category_id = :categoryId)
         AND (:dormitoryId IS NULL OR dorm.id = :dormitoryId)
         AND (:orderExpectedFrom IS NULL OR dp.order_expected_at >= :orderExpectedFrom)
         AND (:orderExpectedTo IS NULL OR dp.order_expected_at <= :orderExpectedTo)
       ORDER BY dp.created_at DESC
      """, nativeQuery = true)
  List<RecruitingDeliveryPartyProjection> findRecruitingDeliveryParties(
      @Param("categoryId") Long categoryId,
      @Param("dormitoryId") Long dormitoryId,
      @Param("orderExpectedFrom") LocalDateTime orderExpectedFrom,
      @Param("orderExpectedTo") LocalDateTime orderExpectedTo
  );

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
