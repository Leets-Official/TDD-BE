package com.leets.tdd.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.CloseDeliveryPartyResponse;
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
    void 파티장이_모집중인_배달팟을_마감한다() {
        DeliveryParty deliveryParty = new DeliveryParty(
                1L, 1L, "치킨 같이 시켜요", "오늘 저녁 배달팟", 2, 4,
                LocalDateTime.of(2026, 7, 24, 19, 30), PartyStatus.RECRUITING,
                null, SettlementStatus.NONE, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(deliveryParty));

        CloseDeliveryPartyResponse response = deliveryPartyService.closeDeliveryParty(1L, 1L);

        assertThat(response.partyId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("CLOSED");
        assertThat(deliveryParty.getStatus()).isEqualTo(PartyStatus.CLOSED);
        assertThat(deliveryParty.getClosedAt()).isNotNull();
    }

    @Test
    void 존재하지_않는_배달팟은_마감할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryPartyService.closeDeliveryParty(999L, 1L))
                .isInstanceOfSatisfying(PartyException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PartyErrorCode.PARTY_NOT_FOUND));
    }

    @Test
    void 파티장이_아니면_모집을_마감할_수_없다() {
        DeliveryParty deliveryParty = new DeliveryParty(
                1L, 1L, "치킨 같이 시켜요", "오늘 저녁 배달팟", 2, 4,
                LocalDateTime.of(2026, 7, 24, 19, 30), PartyStatus.RECRUITING,
                null, SettlementStatus.NONE, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(deliveryParty));

        assertThatThrownBy(() -> deliveryPartyService.closeDeliveryParty(1L, 2L))
                .isInstanceOfSatisfying(PartyException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PartyErrorCode.CLOSE_FORBIDDEN));
    }

    @Test
    void 이미_마감된_배달팟은_다시_마감할_수_없다() {
        DeliveryParty deliveryParty = new DeliveryParty(
                1L, 1L, "치킨 같이 시켜요", "오늘 저녁 배달팟", 2, 4,
                LocalDateTime.of(2026, 7, 24, 19, 30), PartyStatus.CLOSED,
                LocalDateTime.now(), SettlementStatus.NONE, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(deliveryPartyRepository.findWithLockById(1L)).thenReturn(Optional.of(deliveryParty));

        assertThatThrownBy(() -> deliveryPartyService.closeDeliveryParty(1L, 1L))
                .isInstanceOfSatisfying(PartyException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PartyErrorCode.ALREADY_CLOSED));
    }
}
