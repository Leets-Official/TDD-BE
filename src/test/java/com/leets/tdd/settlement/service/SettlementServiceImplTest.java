package com.leets.tdd.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantRole;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.BankAccount;
import com.leets.tdd.settlement.domain.PaymentStatus;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.settlement.dto.request.CreateSettlementRequest;
import com.leets.tdd.settlement.dto.request.SettlementPaymentRequest;
import com.leets.tdd.settlement.dto.response.SettlementDetailResponse;
import com.leets.tdd.settlement.exception.SettlementErrorCode;
import com.leets.tdd.settlement.exception.SettlementException;
import com.leets.tdd.settlement.repository.BankAccountRepository;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

  @Mock
  private DeliveryPartyRepository deliveryPartyRepository;

  @Mock
  private PartyParticipantRepository partyParticipantRepository;

  @Mock
  private BankAccountRepository bankAccountRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private SettlementServiceImpl settlementService;

  @Test
  void 방장이_완료된_팟에_정산을_요청한다() {
    DeliveryParty party = party(10L, 1L, PartyStatus.COMPLETED, SettlementStatus.NONE);
    PartyParticipant host = participant(10L, 1L, PartyParticipantRole.HOST);
    PartyParticipant member = participant(10L, 2L, PartyParticipantRole.MEMBER);
    BankAccount account = org.mockito.Mockito.mock(BankAccount.class);
    User hostUser = user(1L, "방장");
    User memberUser = user(2L, "참여자");

    given(deliveryPartyRepository.findById(10L)).willReturn(Optional.of(party));
    given(partyParticipantRepository.findAllByPartyId(10L)).willReturn(List.of(host, member));
    given(bankAccountRepository.findByUserId(1L)).willReturn(Optional.of(account));
    given(account.getId()).willReturn(100L);
    given(userRepository.findAllByIdIn(any())).willReturn(List.of(hostUser, memberUser));

    SettlementDetailResponse response = settlementService.createSettlement(
        1L,
        10L,
        new CreateSettlementRequest(20_000, List.of(new SettlementPaymentRequest(2L, 12_000)))
    );

    assertThat(response.settlementStatus()).isEqualTo("REQUESTED");
    assertThat(response.hostAmount()).isEqualTo(8_000);
    assertThat(member.getSettlementAmount()).isEqualTo(12_000);
    assertThat(member.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
  }

  @Test
  void 방장은_정산_대상에_포함할_수_없다() {
    DeliveryParty party = party(10L, 1L, PartyStatus.COMPLETED, SettlementStatus.NONE);
    given(deliveryPartyRepository.findById(10L)).willReturn(Optional.of(party));
    given(bankAccountRepository.findByUserId(1L)).willReturn(Optional.of(org.mockito.Mockito.mock(BankAccount.class)));
    given(partyParticipantRepository.findAllByPartyId(10L)).willReturn(List.of(
        participant(10L, 1L, PartyParticipantRole.HOST)
    ));

    assertThatThrownBy(() -> settlementService.createSettlement(
        1L,
        10L,
        new CreateSettlementRequest(10_000, List.of(new SettlementPaymentRequest(1L, 10_000)))
    ))
        .isInstanceOf(SettlementException.class)
        .hasMessage(SettlementErrorCode.HOST_CANNOT_BE_PAYMENT_TARGET.getMessage());
  }

  @Test
  void 송금_완료를_처리하고_되돌린다() {
    DeliveryParty party = requestedParty(10L, 1L);
    PartyParticipant member = participant(10L, 2L, PartyParticipantRole.MEMBER);
    member.assignSettlementAmount(12_000);
    given(deliveryPartyRepository.findById(10L)).willReturn(Optional.of(party));
    given(partyParticipantRepository.findByPartyIdAndUserId(10L, 2L)).willReturn(Optional.of(member));

    settlementService.markMyPaymentPaid(2L, 10L);
    settlementService.undoMyPaymentPaid(2L, 10L);

    assertThat(member.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(member.getPaidAt()).isNull();
  }

  @Test
  void 비방장은_정산을_완료할_수_없다() {
    DeliveryParty party = requestedParty(10L, 1L);
    given(deliveryPartyRepository.findById(10L)).willReturn(Optional.of(party));

    assertThatThrownBy(() -> settlementService.completeSettlement(2L, 10L))
        .isInstanceOf(SettlementException.class)
        .hasMessage(SettlementErrorCode.NOT_HOST.getMessage());
  }

  private DeliveryParty party(Long id, Long creatorId, PartyStatus status, SettlementStatus settlementStatus) {
    DeliveryParty party = new DeliveryParty(
        creatorId,
        1L,
        "치킨 같이 시켜요",
        null,
        2,
        4,
        LocalDateTime.now(),
        status,
        null,
        settlementStatus,
        null,
        null,
        null,
        LocalDateTime.now(),
        LocalDateTime.now()
    );
    ReflectionTestUtils.setField(party, "id", id);
    return party;
  }

  private DeliveryParty requestedParty(Long id, Long creatorId) {
    DeliveryParty party = party(id, creatorId, PartyStatus.COMPLETED, SettlementStatus.NONE);
    party.requestSettlement(20_000, 100L, LocalDateTime.now());
    return party;
  }

  private PartyParticipant participant(Long partyId, Long userId, PartyParticipantRole role) {
    return new PartyParticipant(
        partyId,
        userId,
        role,
        PartyParticipantStatus.JOINED,
        LocalDateTime.now()
    );
  }

  private User user(Long id, String nickname) {
    User user = org.mockito.Mockito.mock(User.class);
    given(user.getId()).willReturn(id);
    given(user.getNickname()).willReturn(nickname);
    return user;
  }
}
