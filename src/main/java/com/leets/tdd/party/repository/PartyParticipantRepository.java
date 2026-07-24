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

  List<PartyParticipant> findAllByPartyIdIn(Collection<Long> partyIds);

  List<PartyParticipant> findAllByUserIdAndPaymentStatusIsNotNull(Long userId);
}
