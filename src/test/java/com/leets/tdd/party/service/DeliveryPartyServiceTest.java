package com.leets.tdd.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.FoodCategory;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.DeliveryPartySearchResponse;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.FoodCategoryRepository;
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
    private FoodCategoryRepository foodCategoryRepository;

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
    void 배달팟_제목으로_검색에_성공한다() {
        DeliveryParty deliveryParty = mock(DeliveryParty.class);
        FoodCategory foodCategory = mock(FoodCategory.class);
        when(deliveryParty.getId()).thenReturn(15L);
        when(deliveryParty.getFoodCategoryId()).thenReturn(1L);
        when(deliveryParty.getTitle()).thenReturn("BBQ 황금올리브 같이 시켜요");
        when(deliveryParty.getMaxParticipants()).thenReturn(4);
        when(deliveryParty.getStatus()).thenReturn(PartyStatus.RECRUITING);
        when(foodCategory.getId()).thenReturn(1L);
        when(foodCategory.getName()).thenReturn("치킨");
        when(deliveryPartyRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc("치킨"))
                .thenReturn(java.util.List.of(deliveryParty));
        when(foodCategoryRepository.findAllById(java.util.List.of(1L)))
                .thenReturn(java.util.List.of(foodCategory));
        when(partyParticipantRepository.countByPartyIdAndStatus(15L, PartyParticipantStatus.JOINED))
                .thenReturn(2L);

        DeliveryPartySearchResponse response = deliveryPartyService.searchDeliveryParties("치킨");

        assertThat(response.parties()).hasSize(1);
        assertThat(response.parties().getFirst().title()).isEqualTo("BBQ 황금올리브 같이 시켜요");
        assertThat(response.parties().getFirst().category()).isEqualTo("치킨");
        assertThat(response.parties().getFirst().currentParticipants()).isEqualTo(2);
    }

    @Test
    void 공백_검색어는_예외를_반환한다() {
        assertThatThrownBy(() -> deliveryPartyService.searchDeliveryParties("  "))
                .isInstanceOf(PartyException.class)
                .hasMessage(PartyErrorCode.SEARCH_KEYWORD_REQUIRED.getMessage());
    }

    @Test
    void 검색_결과가_없으면_예외를_반환한다() {
        when(deliveryPartyRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc("치킨"))
                .thenReturn(java.util.List.of());

        assertThatThrownBy(() -> deliveryPartyService.searchDeliveryParties("치킨"))
                .isInstanceOf(PartyException.class)
                .hasMessage(PartyErrorCode.SEARCH_RESULT_NOT_FOUND.getMessage());
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
}
