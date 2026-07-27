package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
