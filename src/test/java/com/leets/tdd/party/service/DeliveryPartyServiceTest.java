package com.leets.tdd.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.FoodCategory;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.MyPartyStatusFilter;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CompleteDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.CloseDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.DeliveryPartySearchResponse;
import com.leets.tdd.party.dto.response.MyDeliveryPartyListResponse;
import com.leets.tdd.party.dto.response.OrderDeliveryPartyResponse;
import com.leets.tdd.party.repository.projection.MyDeliveryPartyProjection;
import com.leets.tdd.party.dto.response.RecruitingDeliveryPartyListResponse;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.FoodCategoryRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.party.repository.projection.RecruitingDeliveryPartyProjection;
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
    void 참여중인_내_배달팟을_필터와_함께_조회한다() {
        MyDeliveryPartyProjection projection = org.mockito.Mockito.mock(MyDeliveryPartyProjection.class);
        LocalDateTime orderExpectedAt = LocalDateTime.of(2026, 7, 28, 19, 30);
        when(projection.getPartyId()).thenReturn(15L);
        when(projection.getTitle()).thenReturn("치킨 같이 시켜요");
        when(projection.getCategory()).thenReturn("치킨");
        when(projection.getCurrentParticipants()).thenReturn(2L);
        when(projection.getMaxParticipants()).thenReturn(4);
        when(projection.getStatus()).thenReturn("RECRUITING");
        when(projection.getOrderExpectedAt()).thenReturn(orderExpectedAt);
        when(projection.getDormitory()).thenReturn("1기숙사");
        when(partyParticipantRepository.findMyDeliveryParties(
                1L, "ONGOING", 1L, 2L, null, null
        )).thenReturn(java.util.List.of(projection));

        MyDeliveryPartyListResponse response = deliveryPartyService.getMyDeliveryParties(
                1L, MyPartyStatusFilter.ONGOING, 1L, 2L, null, null
        );

        assertThat(response.parties()).hasSize(1);
        assertThat(response.parties().getFirst().partyId()).isEqualTo(15L);
        assertThat(response.parties().getFirst().currentParticipants()).isEqualTo(2);
        assertThat(response.parties().getFirst().dormitory()).isEqualTo("1기숙사");
    }

    @Test
    void 모집중인_배달팟을_필터와_함께_조회한다() {
        RecruitingDeliveryPartyProjection projection = org.mockito.Mockito.mock(
                RecruitingDeliveryPartyProjection.class
        );
        LocalDateTime orderExpectedAt = LocalDateTime.of(2026, 7, 28, 19, 30);
        when(projection.getPartyId()).thenReturn(15L);
        when(projection.getTitle()).thenReturn("치킨 같이 시켜요");
        when(projection.getCategory()).thenReturn("치킨");
        when(projection.getCurrentParticipants()).thenReturn(2L);
        when(projection.getMaxParticipants()).thenReturn(4);
        when(projection.getStatus()).thenReturn("RECRUITING");
        when(projection.getOrderExpectedAt()).thenReturn(orderExpectedAt);
        when(projection.getDormitory()).thenReturn("1기숙사");
        when(deliveryPartyRepository.findRecruitingDeliveryParties(1L, 2L, null, null))
                .thenReturn(java.util.List.of(projection));

        RecruitingDeliveryPartyListResponse response = deliveryPartyService.getRecruitingDeliveryParties(
                1L, 2L, null, null
        );

        assertThat(response.parties()).hasSize(1);
        assertThat(response.parties().getFirst().status()).isEqualTo("RECRUITING");
        assertThat(response.parties().getFirst().category()).isEqualTo("치킨");
        assertThat(response.parties().getFirst().currentParticipants()).isEqualTo(2);
    }

    @Test
    void 배달팟_제목으로_검색에_성공한다() {
        DeliveryParty party = org.mockito.Mockito.mock(DeliveryParty.class);
        FoodCategory category = org.mockito.Mockito.mock(FoodCategory.class);
        when(party.getId()).thenReturn(15L);
        when(party.getFoodCategoryId()).thenReturn(1L);
        when(party.getTitle()).thenReturn("BBQ 황금올리브 같이 시켜요");
        when(party.getMaxParticipants()).thenReturn(4);
        when(party.getStatus()).thenReturn(PartyStatus.RECRUITING);
        when(category.getId()).thenReturn(1L);
        when(category.getName()).thenReturn("치킨");
        when(deliveryPartyRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc("치킨"))
                .thenReturn(java.util.List.of(party));
        when(foodCategoryRepository.findAllById(java.util.List.of(1L)))
                .thenReturn(java.util.List.of(category));
        when(partyParticipantRepository.countByPartyIdAndStatus(15L, PartyParticipantStatus.JOINED))
                .thenReturn(2L);

        DeliveryPartySearchResponse response = deliveryPartyService.searchDeliveryParties("치킨");

        assertThat(response.parties()).hasSize(1);
        assertThat(response.parties().getFirst().title()).isEqualTo("BBQ 황금올리브 같이 시켜요");
        assertThat(response.parties().getFirst().category()).isEqualTo("치킨");
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

    @Test
    void 파티장이_모집중인_배달팟을_마감한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.RECRUITING);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));

        CloseDeliveryPartyResponse response = deliveryPartyService.closeDeliveryParty(10L, 1L);

        assertThat(response.partyId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("CLOSED");
        assertThat(deliveryParty.getClosedAt()).isNotNull();
    }

    @Test
    void 파티장이_아니면_모집을_마감할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.RECRUITING)));

        assertThatThrownBy(() -> deliveryPartyService.closeDeliveryParty(10L, 2L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.CLOSE_FORBIDDEN);
    }

    @Test
    void 이미_마감된_배달팟은_다시_마감할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.CLOSED)));

        assertThatThrownBy(() -> deliveryPartyService.closeDeliveryParty(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.ALREADY_CLOSED);
    }

    @Test
    void 모집_마감된_배달팟의_주문을_완료한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.CLOSED);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));

        OrderDeliveryPartyResponse response = deliveryPartyService.completeOrder(10L, 1L);

        assertThat(response.partyId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("ORDERED");
        assertThat(response.settlementStatus()).isEqualTo("NONE");
        assertThat(deliveryParty.getStatus()).isEqualTo(PartyStatus.ORDERED);
    }

    @Test
    void 파티장이_아니면_주문_완료할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.CLOSED)));

        assertThatThrownBy(() -> deliveryPartyService.completeOrder(10L, 2L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.ORDER_FORBIDDEN);
    }

    @Test
    void 모집_마감된_배달팟만_주문_완료할_수_있다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.RECRUITING)));

        assertThatThrownBy(() -> deliveryPartyService.completeOrder(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.ORDER_NOT_CLOSED);
    }

    @Test
    void 이미_주문_완료된_배달팟은_다시_처리할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.ORDERED)));

        assertThatThrownBy(() -> deliveryPartyService.completeOrder(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.ALREADY_ORDERED);
    }

    private DeliveryParty party(Long id, Long creatorId, PartyStatus status) {
        DeliveryParty party = new DeliveryParty(
                creatorId, 1L, "치킨 같이 시켜요", "오늘 저녁 배달팟", 2, 4,
                LocalDateTime.of(2026, 7, 24, 19, 30), status, null,
                SettlementStatus.NONE, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }
}
