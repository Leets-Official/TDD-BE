package com.leets.tdd.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.CompleteDeliveryPartyResponse;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeliveryPartyServiceTest {

    @Mock
    private DeliveryPartyRepository deliveryPartyRepository;

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
    void 주문_완료된_배달팟의_배달을_완료한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.ORDERED);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));

        CompleteDeliveryPartyResponse response = deliveryPartyService.completeDelivery(10L, 1L);

        assertThat(response.partyId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(deliveryParty.getStatus()).isEqualTo(PartyStatus.COMPLETED);
    }

    @Test
    void 파티장이_아니면_배달_완료할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.ORDERED)));

        assertThatThrownBy(() -> deliveryPartyService.completeDelivery(10L, 2L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.COMPLETE_FORBIDDEN);
    }

    @Test
    void 주문_완료된_배달팟만_배달_완료할_수_있다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.CLOSED)));

        assertThatThrownBy(() -> deliveryPartyService.completeDelivery(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.COMPLETE_NOT_ORDERED);
    }

    @Test
    void 이미_배달_완료된_배달팟은_다시_처리할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.COMPLETED)));

        assertThatThrownBy(() -> deliveryPartyService.completeDelivery(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.ALREADY_COMPLETED);
    }

    private DeliveryParty party(Long id, Long creatorId, PartyStatus status) {
        DeliveryParty party = new DeliveryParty(
                creatorId,
                1L,
                "치킨 같이 시켜요",
                "오늘 저녁 배달팟",
                2,
                4,
                LocalDateTime.of(2026, 7, 24, 19, 30),
                status,
                null,
                SettlementStatus.NONE,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }
}
