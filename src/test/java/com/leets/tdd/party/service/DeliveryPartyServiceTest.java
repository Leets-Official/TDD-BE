package com.leets.tdd.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantRole;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.LeaveDeliveryPartyResponse;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryPartyServiceTest {

    @Mock
    private DeliveryPartyRepository deliveryPartyRepository;

    @Mock
    private PartyParticipantRepository partyParticipantRepository;

    @InjectMocks
    private DeliveryPartyService deliveryPartyService;


    @Test
    void 배달팟_상세조회_성공() {

        // given
        DeliveryParty deliveryParty = new DeliveryParty(
                1L,
                1L,
                "치킨 같이 시켜요",
                "오늘 저녁 배달팟",
                2,
                4,
                LocalDateTime.of(2026, 7, 24, 19, 30),
                PartyStatus.RECRUITING,
                null,
                SettlementStatus.NONE,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );


        when(deliveryPartyRepository.findById(1L))
                .thenReturn(Optional.of(deliveryParty));


        // when
        DeliveryPartyDetailResponse response =
                deliveryPartyService.getDeliveryPartyDetail(1L);


        // then
        assertThat(response.getTitle())
                .isEqualTo("치킨 같이 시켜요");

        assertThat(response.getStatus())
                .isEqualTo("RECRUITING");
    }


    @Test
    void 존재하지_않는_배달팟_조회시_예외발생() {

        // given
        when(deliveryPartyRepository.findById(999L))
                .thenReturn(Optional.empty());


        // when & then
        assertThatThrownBy(() ->
                deliveryPartyService.getDeliveryPartyDetail(999L)
        )
                .isInstanceOf(PartyException.class);
    }


    @Test
    void 배달팟_수정_성공() {

        // given
        DeliveryParty deliveryParty = new DeliveryParty(
                1L,
                1L,
                "치킨 같이 시켜요",
                "오늘 저녁 배달팟",
                2,
                4,
                LocalDateTime.of(2026, 7, 24, 19, 30),
                PartyStatus.RECRUITING,
                null,
                SettlementStatus.NONE,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );


        when(deliveryPartyRepository.findById(1L))
                .thenReturn(Optional.of(deliveryParty));


        UpdateDeliveryPartyRequest request =
                new UpdateDeliveryPartyRequest(
                        "변경된 제목",
                        "변경된 설명",
                        5,
                        LocalDateTime.of(2026, 7, 25, 20, 0)
                );


        // when
        deliveryPartyService.updateDeliveryParty(
                1L,
                request,
                1L
        );


        // then
        verify(deliveryPartyRepository)
                .save(deliveryParty);
        assertThat(deliveryParty.getTitle())
                .isEqualTo("변경된 제목");

        assertThat(deliveryParty.getDescription())
                .isEqualTo("변경된 설명");

        assertThat(deliveryParty.getMaxParticipants())
                .isEqualTo(5);

        assertThat(deliveryParty.getOrderExpectedAt())
                .isEqualTo(LocalDateTime.of(2026, 7, 25, 20, 0));
    }


    @Test
    void 배달팟_참여_취소_성공() {
        // given
        DeliveryParty party = recruitingParty(4);
        PartyParticipant participant = joinedParticipant(1L, 2L);
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(party));
        when(partyParticipantRepository.findByPartyIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(participant));
        when(partyParticipantRepository.countByPartyIdAndStatus(1L, PartyParticipantStatus.JOINED))
                .thenReturn(1L);

        // when
        LeaveDeliveryPartyResponse response = deliveryPartyService.leaveDeliveryParty(1L, 2L);

        // then
        verify(partyParticipantRepository).saveAndFlush(participant);
        assertThat(participant.getStatus()).isEqualTo(PartyParticipantStatus.CANCELED);
        assertThat(participant.getCanceledAt()).isNotNull();
        assertThat(response.partyId()).isEqualTo(1L);
        assertThat(response.currentParticipants()).isEqualTo(2L);
        assertThat(response.maxParticipants()).isEqualTo(4);
    }

    @Test
    void 참여하지_않은_배달팟은_취소할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(recruitingParty(4)));
        when(partyParticipantRepository.findByPartyIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertPartyError(() -> deliveryPartyService.leaveDeliveryParty(1L, 2L),
                PartyErrorCode.NOT_PARTICIPANT);
    }

    @Test
    void 모집중이_아닌_배달팟에서는_참여를_취소할_수_없다() {
        DeliveryParty party = new DeliveryParty(
                1L, 1L, "치킨", "", 2, 4, LocalDateTime.now().plusHours(1),
                PartyStatus.CLOSED, null, SettlementStatus.NONE, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(party));
        when(partyParticipantRepository.findByPartyIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(joinedParticipant(1L, 2L)));

        assertPartyError(() -> deliveryPartyService.leaveDeliveryParty(1L, 2L),
                PartyErrorCode.LEAVE_NOT_ALLOWED);
    }

    @Test
    void 파티장은_참여를_취소할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(recruitingParty(4)));

        assertPartyError(() -> deliveryPartyService.leaveDeliveryParty(1L, 1L),
                PartyErrorCode.HOST_CANNOT_LEAVE);
    }

    private DeliveryParty recruitingParty(int maxParticipants) {
        return new DeliveryParty(
                1L, 1L, "치킨", "", 2, maxParticipants, LocalDateTime.now().plusHours(1),
                PartyStatus.RECRUITING, null, SettlementStatus.NONE, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private PartyParticipant joinedParticipant(Long partyId, Long userId) {
        return new PartyParticipant(
                partyId, userId, PartyParticipantRole.MEMBER,
                PartyParticipantStatus.JOINED, LocalDateTime.now());
    }

    private void assertPartyError(Runnable action, PartyErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PartyException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }
}
