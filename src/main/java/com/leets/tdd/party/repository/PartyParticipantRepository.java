package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.projection.MyDeliveryPartyProjection;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartyParticipantRepository extends JpaRepository<PartyParticipant, Long> {

  boolean existsByPartyIdAndUserIdAndStatus(Long partyId, Long userId, PartyParticipantStatus status);

  List<PartyParticipant> findAllByPartyIdAndStatus(Long partyId, PartyParticipantStatus status);

  Optional<PartyParticipant> findByPartyIdAndUserId(Long partyId, Long userId);

  List<PartyParticipant> findAllByPartyId(Long partyId);

  long countByPartyIdAndStatus(Long partyId, PartyParticipantStatus status);

  List<PartyParticipant> findAllByPartyIdIn(Collection<Long> partyIds);

  List<PartyParticipant> findAllByUserIdAndPaymentStatusIsNotNull(Long userId);

  // 탈퇴 제한: 이 사용자가 참여 중인(취소하지 않은) 팟 목록
  List<PartyParticipant> findAllByUserIdAndStatus(Long userId, PartyParticipantStatus status);

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
        FROM party_participants pp
        JOIN delivery_parties dp ON dp.id = pp.party_id
        JOIN food_categories fc ON fc.id = dp.food_category_id
        LEFT JOIN dormitory dorm ON dorm.user_id = dp.creator_id
       WHERE pp.user_id = :userId
         AND pp.status = 'JOINED'
         AND (:categoryId IS NULL OR dp.food_category_id = :categoryId)
         AND (:dormitory IS NULL OR dorm.dormitory = :dormitory)
         AND (:orderExpectedFrom IS NULL OR dp.order_expected_at >= :orderExpectedFrom)
         AND (:orderExpectedTo IS NULL OR dp.order_expected_at <= :orderExpectedTo)
         AND (:status = 'ALL'
              OR (:status = 'ONGOING' AND dp.status IN ('RECRUITING', 'CLOSED', 'ORDERED'))
              OR (:status = 'COMPLETED' AND dp.status IN ('COMPLETED', 'CANCELED')))
       ORDER BY dp.created_at DESC
      """, nativeQuery = true)
  List<MyDeliveryPartyProjection> findMyDeliveryParties(
      @Param("userId") Long userId,
      @Param("status") String status,
      @Param("categoryId") Long categoryId,
      @Param("dormitory") String dormitory,
      @Param("orderExpectedFrom") LocalDateTime orderExpectedFrom,
      @Param("orderExpectedTo") LocalDateTime orderExpectedTo
  );
}
