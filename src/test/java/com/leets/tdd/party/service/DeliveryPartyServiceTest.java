package com.leets.tdd.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantRole;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.MyPartyStatusFilter;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.JoinDeliveryPartyResponse;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.repository.DormitoryRepository;
import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private DormitoryRepository dormitoryRepository;

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
    void 배달팟_삭제_성공() {

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
        Long result = null;

        try {
            result = deliveryPartyService.deleteDeliveryParty(1L, 1L);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }


        // then
        assertThat(result)
                .isEqualTo(1L);
    }


    @Test
    void 배달팟_참여_성공() {
        // given
        DeliveryParty party = recruitingParty(4);
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(party));
        when(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
                1L, 2L, PartyParticipantStatus.JOINED)).thenReturn(false);
        when(partyParticipantRepository.countByPartyIdAndStatus(
                1L, PartyParticipantStatus.JOINED)).thenReturn(1L);

        // when
        JoinDeliveryPartyResponse response = deliveryPartyService.joinDeliveryParty(1L, 2L);

        // then
        verify(partyParticipantRepository).saveAndFlush(any(PartyParticipant.class));
        assertThat(response.partyId()).isEqualTo(1L);
        assertThat(response.currentParticipants()).isEqualTo(3L);
        assertThat(response.maxParticipants()).isEqualTo(4);
    }

    @Test
    void 존재하지_않는_배달팟에는_참여할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(999L)).thenReturn(Optional.empty());

        assertPartyError(() -> deliveryPartyService.joinDeliveryParty(999L, 2L),
                PartyErrorCode.PARTY_NOT_FOUND);
    }

    @Test
    void 이미_참여한_배달팟에는_다시_참여할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(recruitingParty(4)));
        when(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
                1L, 2L, PartyParticipantStatus.JOINED)).thenReturn(true);

        assertPartyError(() -> deliveryPartyService.joinDeliveryParty(1L, 2L),
                PartyErrorCode.ALREADY_JOINED);
    }

    @Test
    void 모집중이_아닌_배달팟에는_참여할_수_없다() {
        DeliveryParty party = new DeliveryParty(
                1L, 1L, "치킨", "", 2, 4, LocalDateTime.now().plusHours(1),
                PartyStatus.CLOSED, null, SettlementStatus.NONE, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(party));

        assertPartyError(() -> deliveryPartyService.joinDeliveryParty(1L, 2L),
                PartyErrorCode.RECRUITMENT_CLOSED);
    }

    @Test
    void 정원이_찬_배달팟에는_참여할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(recruitingParty(2)));
        when(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
                1L, 2L, PartyParticipantStatus.JOINED)).thenReturn(false);
        when(partyParticipantRepository.countByPartyIdAndStatus(
                1L, PartyParticipantStatus.JOINED)).thenReturn(1L);

        assertPartyError(() -> deliveryPartyService.joinDeliveryParty(1L, 2L),
                PartyErrorCode.PARTY_FULL);
    }

    @Test
    void 메인_배달팟_목록은_모집중인_팟만_필터링한다() {
        DeliveryParty recruitingParty = recruitingParty(4);
        DeliveryParty closedParty = new DeliveryParty(
                2L, 1L, "마감된 팟", "", 2, 4, LocalDateTime.now().plusHours(1),
                PartyStatus.CLOSED, null, SettlementStatus.NONE, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(deliveryPartyRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(recruitingParty, closedParty));
        when(dormitoryRepository.findAllByUserIdIn(any()))
                .thenReturn(List.of(new Dormitory(1L, "A동", null), new Dormitory(2L, "A동", null)));

        List<CreateDeliveryPartyResponse> response = deliveryPartyService.getDeliveryParties(
                1L, "A동", null, null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getTitle()).isEqualTo("치킨");
        assertThat(response.getFirst().getStatus()).isEqualTo("RECRUITING");
    }

    @Test
    void 내_배달팟_목록은_참여자_기준으로_진행중_상태만_조회한다() {
        DeliveryParty recruitingParty = recruitingParty(4);
        DeliveryParty completedParty = new DeliveryParty(
                2L, 1L, "완료된 팟", "", 2, 4, LocalDateTime.now().plusHours(1),
                PartyStatus.COMPLETED, null, SettlementStatus.NONE, null, null, null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(partyParticipantRepository.findAllByUserIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(List.of(
                        new PartyParticipant(1L, 10L, PartyParticipantRole.MEMBER,
                                PartyParticipantStatus.JOINED, LocalDateTime.now()),
                        new PartyParticipant(2L, 10L, PartyParticipantRole.MEMBER,
                                PartyParticipantStatus.JOINED, LocalDateTime.now())
                ));
        when(deliveryPartyRepository.findAllById(any())).thenReturn(List.of(recruitingParty, completedParty));
        when(dormitoryRepository.findAllByUserIdIn(any()))
                .thenReturn(List.of(new Dormitory(1L, "A동", null), new Dormitory(2L, "A동", null)));

        List<CreateDeliveryPartyResponse> response = deliveryPartyService.getMyDeliveryParties(
                10L, null, null, null, null, MyPartyStatusFilter.IN_PROGRESS);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getStatus()).isEqualTo("RECRUITING");
    }

    private DeliveryParty recruitingParty(int maxParticipants) {
        return new DeliveryParty(
                1L, 1L, "치킨", "", 2, maxParticipants, LocalDateTime.now().plusHours(1),
                PartyStatus.RECRUITING, null, SettlementStatus.NONE, null, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private void assertPartyError(Runnable action, PartyErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PartyException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }
}
