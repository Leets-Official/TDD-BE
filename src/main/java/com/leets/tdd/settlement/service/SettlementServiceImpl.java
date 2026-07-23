package com.leets.tdd.settlement.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.BankAccount;
import com.leets.tdd.settlement.domain.PaymentStatus;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.settlement.dto.request.CreateSettlementRequest;
import com.leets.tdd.settlement.dto.request.SettlementPaymentRequest;
import com.leets.tdd.settlement.dto.response.MyIncomingSettlementResponse;
import com.leets.tdd.settlement.dto.response.MyOutgoingSettlementResponse;
import com.leets.tdd.settlement.dto.response.MySettlementListResponse;
import com.leets.tdd.settlement.dto.response.MySettlementSummaryResponse;
import com.leets.tdd.settlement.dto.response.PaymentStatusResponse;
import com.leets.tdd.settlement.dto.response.SettlementBankAccountResponse;
import com.leets.tdd.settlement.dto.response.SettlementCancelResponse;
import com.leets.tdd.settlement.dto.response.SettlementCompletionResponse;
import com.leets.tdd.settlement.dto.response.SettlementDetailResponse;
import com.leets.tdd.settlement.dto.response.SettlementPaymentResponse;
import com.leets.tdd.settlement.dto.response.SettlementRequesterResponse;
import com.leets.tdd.settlement.exception.SettlementErrorCode;
import com.leets.tdd.settlement.exception.SettlementException;
import com.leets.tdd.settlement.repository.BankAccountRepository;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

  private final DeliveryPartyRepository deliveryPartyRepository;
  private final PartyParticipantRepository partyParticipantRepository;
  private final BankAccountRepository bankAccountRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public SettlementDetailResponse createSettlement(
      Long currentUserId,
      Long partyId,
      CreateSettlementRequest request
  ) {
    DeliveryParty party = getParty(partyId);
    validateHost(party, currentUserId);
    validateSettlementCreatable(party);
    BankAccount bankAccount = bankAccountRepository.findByUserId(currentUserId)
        .orElseThrow(() -> new SettlementException(SettlementErrorCode.BANK_ACCOUNT_NOT_FOUND));
    List<PartyParticipant> participants = partyParticipantRepository.findAllByPartyId(partyId);
    Map<Long, PartyParticipant> joinedParticipants = joinedParticipantsByUserId(participants);
    validatePayments(party, request, joinedParticipants);

    participants.forEach(PartyParticipant::clearSettlement);
    request.payments().forEach(payment -> joinedParticipants.get(payment.userId())
        .assignSettlementAmount(payment.amount()));
    party.requestSettlement(request.totalAmount(), bankAccount.getId(), LocalDateTime.now());

    return toSettlementDetailResponse(party, bankAccount, participants);
  }

  @Override
  public SettlementDetailResponse getSettlement(Long currentUserId, Long partyId) {
    DeliveryParty party = getParty(partyId);
    validateJoinedParticipant(partyId, currentUserId);
    List<PartyParticipant> participants = partyParticipantRepository.findAllByPartyId(partyId);
    if (party.getSettlementStatus() == SettlementStatus.NONE) {
      return toSettlementDetailResponse(party, null, participants);
    }
    BankAccount bankAccount = party.getSettlementBankAccountId() == null
        ? null
        : bankAccountRepository.findById(party.getSettlementBankAccountId()).orElse(null);
    return toSettlementDetailResponse(party, bankAccount, participants);
  }

  @Override
  @Transactional
  public PaymentStatusResponse markMyPaymentPaid(Long currentUserId, Long partyId) {
    DeliveryParty party = getParty(partyId);
    PartyParticipant participant = getJoinedParticipant(partyId, currentUserId);
    validateSettlementRequested(party);
    if (participant.getPaymentStatus() == null || participant.getSettlementAmount() == null) {
      throw new SettlementException(SettlementErrorCode.PAYMENT_NOT_FOUND);
    }
    if (participant.getPaymentStatus() == PaymentStatus.PAID) {
      throw new SettlementException(SettlementErrorCode.PAYMENT_ALREADY_COMPLETED);
    }
    participant.markPaid(LocalDateTime.now());
    return toPaymentStatusResponse(partyId, participant);
  }

  @Override
  @Transactional
  public PaymentStatusResponse undoMyPaymentPaid(Long currentUserId, Long partyId) {
    DeliveryParty party = getParty(partyId);
    PartyParticipant participant = getJoinedParticipant(partyId, currentUserId);
    validateSettlementRequested(party);
    if (participant.getPaymentStatus() == null || participant.getSettlementAmount() == null) {
      throw new SettlementException(SettlementErrorCode.PAYMENT_NOT_FOUND);
    }
    if (participant.getPaymentStatus() != PaymentStatus.PAID) {
      throw new SettlementException(SettlementErrorCode.PAYMENT_NOT_COMPLETED);
    }
    participant.undoPaid();
    return toPaymentStatusResponse(partyId, participant);
  }

  @Override
  @Transactional
  public SettlementCompletionResponse completeSettlement(Long currentUserId, Long partyId) {
    DeliveryParty party = getParty(partyId);
    validateHost(party, currentUserId);
    validateSettlementRequested(party);
    long unpaidCount = partyParticipantRepository.findAllByPartyId(partyId).stream()
        .filter(PartyParticipant::isJoined)
        .filter(participant -> participant.getPaymentStatus() == PaymentStatus.PENDING)
        .count();
    party.completeSettlement();
    return new SettlementCompletionResponse(partyId, party.getSettlementStatus().name(), unpaidCount);
  }

  @Override
  @Transactional
  public SettlementCancelResponse cancelSettlement(Long currentUserId, Long partyId) {
    DeliveryParty party = getParty(partyId);
    validateHost(party, currentUserId);
    validateSettlementRequested(party);
    long paidCount = partyParticipantRepository.findAllByPartyId(partyId).stream()
        .filter(PartyParticipant::isJoined)
        .filter(participant -> participant.getPaymentStatus() == PaymentStatus.PAID)
        .count();
    party.cancelSettlement();
    return new SettlementCancelResponse(partyId, party.getSettlementStatus().name(), paidCount);
  }

  @Override
  public MySettlementListResponse getMySettlements(Long currentUserId) {
    List<PartyParticipant> myPayments = partyParticipantRepository
        .findAllByUserIdAndPaymentStatusIsNotNull(currentUserId).stream()
        .filter(PartyParticipant::isJoined)
        .toList();
    Map<Long, DeliveryParty> partiesById = deliveryPartyRepository.findAllById(
            myPayments.stream().map(PartyParticipant::getPartyId).collect(Collectors.toSet())
        ).stream()
        .collect(Collectors.toMap(DeliveryParty::getId, party -> party));
    Map<Long, User> usersById = usersById(partiesById.values().stream()
        .map(DeliveryParty::getCreatorId)
        .collect(Collectors.toSet()));

    List<MyOutgoingSettlementResponse> outgoing = myPayments.stream()
        .map(participant -> toOutgoingResponse(participant, partiesById.get(participant.getPartyId()), usersById))
        .filter(response -> response != null)
        .toList();
    long pendingCount = outgoing.stream()
        .filter(response -> PaymentStatus.PENDING.name().equals(response.paymentStatus()))
        .count();
    long pendingAmount = outgoing.stream()
        .filter(response -> PaymentStatus.PENDING.name().equals(response.paymentStatus()))
        .mapToLong(MyOutgoingSettlementResponse::amount)
        .sum();

    List<MyIncomingSettlementResponse> incoming = deliveryPartyRepository
        .findAllByCreatorIdAndSettlementStatus(currentUserId, SettlementStatus.REQUESTED)
        .stream()
        .map(this::toIncomingResponse)
        .toList();

    return new MySettlementListResponse(
        new MySettlementSummaryResponse(pendingCount, pendingAmount),
        outgoing,
        incoming
    );
  }

  private DeliveryParty getParty(Long partyId) {
    return deliveryPartyRepository.findById(partyId)
        .orElseThrow(() -> new SettlementException(SettlementErrorCode.PARTY_NOT_FOUND));
  }

  private PartyParticipant getJoinedParticipant(Long partyId, Long userId) {
    PartyParticipant participant = partyParticipantRepository.findByPartyIdAndUserId(partyId, userId)
        .orElseThrow(() -> new SettlementException(SettlementErrorCode.NOT_PARTICIPANT));
    if (!participant.isJoined()) {
      throw new SettlementException(SettlementErrorCode.NOT_PARTICIPANT);
    }
    return participant;
  }

  private void validateJoinedParticipant(Long partyId, Long userId) {
    getJoinedParticipant(partyId, userId);
  }

  private void validateHost(DeliveryParty party, Long currentUserId) {
    if (!party.getCreatorId().equals(currentUserId)) {
      throw new SettlementException(SettlementErrorCode.NOT_HOST);
    }
  }

  private void validateSettlementCreatable(DeliveryParty party) {
    if (party.getStatus() != PartyStatus.COMPLETED) {
      throw new SettlementException(SettlementErrorCode.PARTY_NOT_COMPLETED);
    }
    if (party.getSettlementStatus() == SettlementStatus.REQUESTED) {
      throw new SettlementException(SettlementErrorCode.SETTLEMENT_ALREADY_REQUESTED);
    }
    if (party.getSettlementStatus() == SettlementStatus.COMPLETED) {
      throw new SettlementException(SettlementErrorCode.SETTLEMENT_ALREADY_COMPLETED);
    }
  }

  private void validateSettlementRequested(DeliveryParty party) {
    if (party.getSettlementStatus() != SettlementStatus.REQUESTED) {
      throw new SettlementException(SettlementErrorCode.SETTLEMENT_NOT_REQUESTED);
    }
  }

  private Map<Long, PartyParticipant> joinedParticipantsByUserId(List<PartyParticipant> participants) {
    return participants.stream()
        .filter(PartyParticipant::isJoined)
        .collect(Collectors.toMap(PartyParticipant::getUserId, participant -> participant));
  }

  private void validatePayments(
      DeliveryParty party,
      CreateSettlementRequest request,
      Map<Long, PartyParticipant> joinedParticipants
  ) {
    Set<Long> targetUserIds = new HashSet<>();
    long paymentSum = 0;
    for (SettlementPaymentRequest payment : request.payments()) {
      if (!targetUserIds.add(payment.userId())) {
        throw new SettlementException(SettlementErrorCode.DUPLICATE_PAYMENT_TARGET);
      }
      if (party.getCreatorId().equals(payment.userId())) {
        throw new SettlementException(SettlementErrorCode.HOST_CANNOT_BE_PAYMENT_TARGET);
      }
      if (!joinedParticipants.containsKey(payment.userId())) {
        throw new SettlementException(SettlementErrorCode.INVALID_PAYMENT_TARGET);
      }
      paymentSum += payment.amount();
    }
    if (paymentSum > request.totalAmount()) {
      throw new SettlementException(SettlementErrorCode.PAYMENT_SUM_EXCEEDED);
    }
  }

  private SettlementDetailResponse toSettlementDetailResponse(
      DeliveryParty party,
      BankAccount bankAccount,
      List<PartyParticipant> participants
  ) {
    Map<Long, User> usersById = usersById(participants.stream()
        .map(PartyParticipant::getUserId)
        .collect(Collectors.toSet()));
    User host = usersById.get(party.getCreatorId());
    List<SettlementPaymentResponse> payments = participants.stream()
        .filter(PartyParticipant::isJoined)
        .filter(participant -> participant.getPaymentStatus() != null)
        .map(participant -> new SettlementPaymentResponse(
            participant.getUserId(),
            nicknameOf(usersById, participant.getUserId()),
            participant.getSettlementAmount(),
            participant.getPaymentStatus().name(),
            participant.getPaidAt()
        ))
        .toList();
    int paymentSum = payments.stream().mapToInt(SettlementPaymentResponse::amount).sum();
    Integer totalAmount = party.getSettlementTotalAmount();
    Integer hostAmount = totalAmount == null ? null : totalAmount - paymentSum;

    return new SettlementDetailResponse(
        party.getId(),
        party.getSettlementStatus().name(),
        totalAmount,
        hostAmount,
        party.getSettlementRequestedAt(),
        new SettlementRequesterResponse(party.getCreatorId(), host == null ? null : host.getNickname()),
        bankAccount == null ? null : new SettlementBankAccountResponse(
            bankAccount.getBankName(), bankAccount.getAccountNumber(), bankAccount.getAccountHolder()
        ),
        payments
    );
  }

  private PaymentStatusResponse toPaymentStatusResponse(Long partyId, PartyParticipant participant) {
    return new PaymentStatusResponse(
        partyId,
        participant.getUserId(),
        participant.getSettlementAmount(),
        participant.getPaymentStatus().name(),
        participant.getPaidAt()
    );
  }

  private MyOutgoingSettlementResponse toOutgoingResponse(
      PartyParticipant participant,
      DeliveryParty party,
      Map<Long, User> usersById
  ) {
    if (party == null || party.getSettlementStatus() != SettlementStatus.REQUESTED) {
      return null;
    }
    return new MyOutgoingSettlementResponse(
        party.getId(),
        party.getTitle(),
        nicknameOf(usersById, party.getCreatorId()),
        participant.getSettlementAmount(),
        participant.getPaymentStatus().name(),
        party.getSettlementRequestedAt()
    );
  }

  private MyIncomingSettlementResponse toIncomingResponse(DeliveryParty party) {
    List<PartyParticipant> participants = partyParticipantRepository.findAllByPartyId(party.getId());
    List<PartyParticipant> paymentParticipants = participants.stream()
        .filter(PartyParticipant::isJoined)
        .filter(participant -> participant.getPaymentStatus() != null)
        .toList();
    long paidCount = paymentParticipants.stream()
        .filter(participant -> participant.getPaymentStatus() == PaymentStatus.PAID)
        .count();
    return new MyIncomingSettlementResponse(
        party.getId(),
        party.getTitle(),
        party.getSettlementTotalAmount(),
        paidCount,
        paymentParticipants.size(),
        party.getSettlementRequestedAt()
    );
  }

  private Map<Long, User> usersById(Set<Long> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findAllByIdIn(userIds).stream()
        .collect(Collectors.toMap(User::getId, user -> user));
  }

  private String nicknameOf(Map<Long, User> usersById, Long userId) {
    User user = usersById.get(userId);
    return user == null ? null : user.getNickname();
  }
}
