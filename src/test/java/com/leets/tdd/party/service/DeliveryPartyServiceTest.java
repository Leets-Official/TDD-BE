package com.leets.tdd.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.chat.service.ChatService;
import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.party.domain.FoodCategory;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantRole;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.MyPartyStatusFilter;
import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CompleteDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.CompleteMvpSettlementResponse;
import com.leets.tdd.party.dto.response.CloseDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.DeliveryPartySearchResponse;
import com.leets.tdd.party.dto.response.MyDeliveryPartyListResponse;
import com.leets.tdd.party.dto.response.OrderDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.PartyParticipantListResponse;
import com.leets.tdd.party.repository.projection.MyDeliveryPartyProjection;
import com.leets.tdd.party.dto.response.RecruitingDeliveryPartyListResponse;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.FoodCategoryRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.party.repository.projection.RecruitingDeliveryPartyProjection;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeliveryPartyServiceTest {

    @Mock
    private DeliveryPartyRepository deliveryPartyRepository;

    @Mock
    private FoodCategoryRepository foodCategoryRepository;

    @Mock
    private PartyParticipantRepository partyParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ChatService chatService;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private DeliveryPartyService deliveryPartyService;

    @Test
    void 배달팟_생성시_로그인한_사용자를_방장과_첫_참여자로_등록한다() {
        CreateDeliveryPartyRequest request = new CreateDeliveryPartyRequest();
        ReflectionTestUtils.setField(request, "foodCategoryId", 1L);
        ReflectionTestUtils.setField(request, "dormitory", "1기숙사");
        ReflectionTestUtils.setField(request, "title", "치킨 같이 시켜요");
        ReflectionTestUtils.setField(request, "minParticipants", 2);
        ReflectionTestUtils.setField(request, "maxParticipants", 4);
        ReflectionTestUtils.setField(request, "orderExpectedAt", LocalDateTime.of(2026, 8, 1, 19, 0));
        when(deliveryPartyRepository.save(org.mockito.ArgumentMatchers.any(DeliveryParty.class)))
                .thenAnswer(invocation -> {
                    DeliveryParty party = invocation.getArgument(0);
                    ReflectionTestUtils.setField(party, "id", 15L);
                    return party;
                });

        deliveryPartyService.createDeliveryParty(request, 2L);

        ArgumentCaptor<DeliveryParty> partyCaptor = ArgumentCaptor.forClass(DeliveryParty.class);
        ArgumentCaptor<PartyParticipant> participantCaptor = ArgumentCaptor.forClass(PartyParticipant.class);
        verify(deliveryPartyRepository).save(partyCaptor.capture());
        verify(partyParticipantRepository).save(participantCaptor.capture());
        assertThat(partyCaptor.getValue().getCreatorId()).isEqualTo(2L);
        assertThat(partyCaptor.getValue().getDormitory()).isEqualTo("1기숙사");
        assertThat(participantCaptor.getValue().getPartyId()).isEqualTo(15L);
        assertThat(participantCaptor.getValue().getUserId()).isEqualTo(2L);
        assertThat(participantCaptor.getValue().getRole()).isEqualTo(PartyParticipantRole.HOST);
        assertThat(participantCaptor.getValue().getStatus()).isEqualTo(PartyParticipantStatus.JOINED);
        verifyNoInteractions(userRepository);
    }


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
        User leader = new User(
                "leader@test.com",
                "방장",
                "password",
                "",
                LocalDateTime.now()
        );
        leader.updateProfile("방장", "profiles/1/leader.jpg");
        ReflectionTestUtils.setField(leader, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(imageStorageService.resolveViewUrl("profiles/1/leader.jpg"))
                .thenReturn("https://cdn.example.com/profiles/1/leader.jpg");
        ReflectionTestUtils.setField(deliveryParty, "dormitory", "1기숙사");


        // when
        DeliveryPartyDetailResponse response =
                deliveryPartyService.getDeliveryPartyDetail(1L);


        // then
        assertThat(response.getTitle())
                .isEqualTo("치킨 같이 시켜요");

        assertThat(response.getStatus())
                .isEqualTo("RECRUITING");
        assertThat(response.getLeaderNickname()).isEqualTo("방장");
        assertThat(response.getLeaderProfileImage())
                .isEqualTo("https://cdn.example.com/profiles/1/leader.jpg");
        assertThat(response.getLeaderMannerTemperature()).isEqualByComparingTo("3.0");
        assertThat(response.getDormitory()).isEqualTo("1기숙사");
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
    void 참여자_목록에_매너온도를_포함한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.RECRUITING);
        User owner = new User("owner@test.com", "방장", "password", "", LocalDateTime.now());
        User member = new User("member@test.com", "참여자", "password", "", LocalDateTime.now());
        owner.updateProfile("방장", "profiles/1/owner.jpg");
        member.updateProfile("참여자", "profiles/2/member.jpg");
        ReflectionTestUtils.setField(owner, "id", 1L);
        ReflectionTestUtils.setField(member, "id", 2L);
        PartyParticipant joinedOwner = new PartyParticipant(
                10L, 1L, PartyParticipantRole.HOST, PartyParticipantStatus.JOINED, LocalDateTime.now()
        );
        PartyParticipant joinedMember = new PartyParticipant(
                10L, 2L, PartyParticipantRole.MEMBER, PartyParticipantStatus.JOINED, LocalDateTime.now()
        );
        when(deliveryPartyRepository.findById(10L)).thenReturn(Optional.of(deliveryParty));
        when(partyParticipantRepository.findAllByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(java.util.List.of(joinedMember, joinedOwner));
        when(userRepository.findAllByIdIn(java.util.List.of(2L, 1L)))
                .thenReturn(java.util.List.of(owner, member));
        when(imageStorageService.resolveViewUrl("profiles/1/owner.jpg"))
                .thenReturn("https://cdn.example.com/profiles/1/owner.jpg");
        when(imageStorageService.resolveViewUrl("profiles/2/member.jpg"))
                .thenReturn("https://cdn.example.com/profiles/2/member.jpg");

        PartyParticipantListResponse response = deliveryPartyService.getPartyParticipants(10L);

        assertThat(response.participants()).hasSize(2);
        assertThat(response.participants().get(0).role()).isEqualTo("OWNER");
        assertThat(response.participants().get(1).role()).isEqualTo("MEMBER");
        assertThat(response.participants().get(0).mannerTemperature()).isEqualByComparingTo("3.0");
        assertThat(response.participants().get(1).mannerTemperature()).isEqualByComparingTo("3.0");
        assertThat(response.participants().get(0).profileImage())
                .isEqualTo("https://cdn.example.com/profiles/1/owner.jpg");
        assertThat(response.participants().get(1).profileImage())
                .isEqualTo("https://cdn.example.com/profiles/2/member.jpg");
    }

    @Test
    void 참여중인_내_배달팟을_필터와_함께_조회한다() {
        MyDeliveryPartyProjection projection = org.mockito.Mockito.mock(MyDeliveryPartyProjection.class);
        LocalDateTime orderExpectedAt = LocalDateTime.of(2026, 7, 28, 19, 30);
        when(projection.getPartyId()).thenReturn(15L);
        when(projection.getTitle()).thenReturn("치킨 같이 시켜요");
        when(projection.getCategory()).thenReturn("치킨");
        when(projection.getCurrentParticipants()).thenReturn(2L);
        when(projection.getMinParticipants()).thenReturn(2);
        when(projection.getMaxParticipants()).thenReturn(4);
        when(projection.getStatus()).thenReturn("RECRUITING");
        when(projection.getOrderExpectedAt()).thenReturn(orderExpectedAt);
        when(projection.getDormitory()).thenReturn("1기숙사");
        when(partyParticipantRepository.findMyDeliveryParties(
                1L, "ONGOING", 1L, "1기숙사", null, null
        )).thenReturn(java.util.List.of(projection));

        MyDeliveryPartyListResponse response = deliveryPartyService.getMyDeliveryParties(
                1L, MyPartyStatusFilter.ONGOING, 1L, "1기숙사", null, null
        );

        assertThat(response.parties()).hasSize(1);
        assertThat(response.parties().getFirst().partyId()).isEqualTo(15L);
        assertThat(response.parties().getFirst().currentParticipants()).isEqualTo(2);
        assertThat(response.parties().getFirst().minParticipants()).isEqualTo(2);
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
        when(projection.getMinParticipants()).thenReturn(2);
        when(projection.getMaxParticipants()).thenReturn(4);
        when(projection.getStatus()).thenReturn("RECRUITING");
        when(projection.getOrderExpectedAt()).thenReturn(orderExpectedAt);
        when(projection.getDormitory()).thenReturn("1기숙사");
        when(deliveryPartyRepository.findRecruitingDeliveryParties(1L, "1기숙사", null, null))
                .thenReturn(java.util.List.of(projection));

        RecruitingDeliveryPartyListResponse response = deliveryPartyService.getRecruitingDeliveryParties(
                1L, "1기숙사", null, null
        );

        assertThat(response.parties()).hasSize(1);
        assertThat(response.parties().getFirst().status()).isEqualTo("RECRUITING");
        assertThat(response.parties().getFirst().category()).isEqualTo("치킨");
        assertThat(response.parties().getFirst().currentParticipants()).isEqualTo(2);
        assertThat(response.parties().getFirst().minParticipants()).isEqualTo(2);
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
        assertThat(response.status()).isEqualTo("DELIVERED");
        assertThat(deliveryParty.getStatus()).isEqualTo(PartyStatus.DELIVERED);
        assertNotificationPublished(deliveryParty, DeliveryPartyNotificationType.DELIVERY_COMPLETED);
    }

    @Test
    void 파티장이_아니면_배달_완료할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.ORDERED)));

        assertThatThrownBy(() -> deliveryPartyService.completeDelivery(10L, 2L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.COMPLETE_FORBIDDEN);
        verifyNoInteractions(eventPublisher);
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
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.DELIVERED)));

        assertThatThrownBy(() -> deliveryPartyService.completeDelivery(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.ALREADY_DELIVERED);
    }

    @Test
    void 파티장이_모집중인_배달팟을_마감한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.RECRUITING);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));
        when(partyParticipantRepository.countByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(2L);

        CloseDeliveryPartyResponse response = deliveryPartyService.closeDeliveryParty(10L, 1L);

        assertThat(response.partyId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("CLOSED");
        assertThat(deliveryParty.getClosedAt()).isNotNull();
        verify(chatService).createChatRoom(10L);
        assertNotificationPublished(deliveryParty, DeliveryPartyNotificationType.RECRUITMENT_CLOSED);
    }

    @Test
    void 목표_인원에_도달하면_자동으로_모집을_마감한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.RECRUITING);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));
        when(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
                10L, 2L, PartyParticipantStatus.JOINED
        )).thenReturn(false);
        when(partyParticipantRepository.countByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(3L, 4L);

        deliveryPartyService.joinDeliveryParty(10L, 2L);

        assertThat(deliveryParty.getStatus()).isEqualTo(PartyStatus.CLOSED);
        verify(chatService).createChatRoom(10L);
        ArgumentCaptor<DeliveryPartyNotificationEvent> captor = ArgumentCaptor.forClass(
                DeliveryPartyNotificationEvent.class
        );
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DeliveryPartyNotificationEvent::type)
                .containsExactly(
                        DeliveryPartyNotificationType.PARTICIPANT_JOINED,
                        DeliveryPartyNotificationType.RECRUITMENT_CLOSED
                );
    }

    @Test
    void 참여자가_2명_미만이면_조기_모집_마감할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.RECRUITING)));
        when(partyParticipantRepository.countByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(1L);

        assertThatThrownBy(() -> deliveryPartyService.closeDeliveryParty(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.CLOSE_MIN_PARTICIPANTS);
    }

    @Test
    void 최소_모집_인원에_도달하지_않으면_조기_모집_마감할_수_없다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.RECRUITING);
        ReflectionTestUtils.setField(deliveryParty, "minParticipants", 3);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));
        when(partyParticipantRepository.countByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(2L);

        assertThatThrownBy(() -> deliveryPartyService.closeDeliveryParty(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.CLOSE_MIN_PARTICIPANTS);
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
        assertNotificationPublished(deliveryParty, DeliveryPartyNotificationType.ORDER_COMPLETED);
    }

    @Test
    void 파티장이_아니면_주문_완료할_수_없다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.CLOSED)));

        assertThatThrownBy(() -> deliveryPartyService.completeOrder(10L, 2L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.ORDER_FORBIDDEN);
        verifyNoInteractions(eventPublisher);
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

    @Test
    void 배달_도착된_배달팟을_MVP_정산_완료한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.DELIVERED);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));

        CompleteMvpSettlementResponse response = deliveryPartyService.completeMvpSettlement(10L, 1L);

        assertThat(response.partyId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("SETTLED");
        assertThat(deliveryParty.getStatus()).isEqualTo(PartyStatus.SETTLED);
    }

    @Test
    void 배달_도착된_배달팟만_MVP_정산_완료할_수_있다() {
        when(deliveryPartyRepository.findWithLockById(10L))
                .thenReturn(Optional.of(party(10L, 1L, PartyStatus.ORDERED)));

        assertThatThrownBy(() -> deliveryPartyService.completeMvpSettlement(10L, 1L))
                .isInstanceOf(PartyException.class)
                .extracting(exception -> ((PartyException) exception).getErrorCode())
                .isEqualTo(PartyErrorCode.SETTLEMENT_NOT_DELIVERED);
    }

    @Test
    void 참여자_추가_후_참여_알림_이벤트를_발행한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.RECRUITING);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));
        when(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
                10L, 2L, PartyParticipantStatus.JOINED
        )).thenReturn(false);
        when(partyParticipantRepository.countByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(1L);

        deliveryPartyService.joinDeliveryParty(10L, 2L);

        assertNotificationPublished(deliveryParty, DeliveryPartyNotificationType.PARTICIPANT_JOINED);
    }

    @Test
    void 참여_취소_후_퇴장_알림_이벤트를_발행한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.RECRUITING);
        PartyParticipant participant = new PartyParticipant(
                10L, 2L, PartyParticipantRole.MEMBER, PartyParticipantStatus.JOINED, LocalDateTime.now()
        );
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));
        when(partyParticipantRepository.findByPartyIdAndUserId(10L, 2L)).thenReturn(Optional.of(participant));
        when(partyParticipantRepository.countByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
                .thenReturn(0L);

        deliveryPartyService.leaveDeliveryParty(10L, 2L);

        assertNotificationPublished(deliveryParty, DeliveryPartyNotificationType.PARTICIPANT_LEFT);
    }

    @Test
    void 배달팟_취소_후_취소_알림_이벤트를_발행한다() {
        DeliveryParty deliveryParty = party(10L, 1L, PartyStatus.RECRUITING);
        when(deliveryPartyRepository.findWithLockById(10L)).thenReturn(Optional.of(deliveryParty));

        deliveryPartyService.deleteDeliveryParty(10L, 1L);

        assertNotificationPublished(deliveryParty, DeliveryPartyNotificationType.PARTY_CANCELED);
    }

    private void assertNotificationPublished(
            DeliveryParty expectedParty,
            DeliveryPartyNotificationType expectedType
    ) {
        ArgumentCaptor<DeliveryPartyNotificationEvent> captor = ArgumentCaptor.forClass(
                DeliveryPartyNotificationEvent.class
        );
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().party()).isSameAs(expectedParty);
        assertThat(captor.getValue().type()).isEqualTo(expectedType);
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
