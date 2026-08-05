package com.leets.tdd.party.service;

import com.leets.tdd.chat.domain.MessageType;
import com.leets.tdd.chat.service.ChatService;
import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantRole;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.dto.MyPartyStatusFilter;
import com.leets.tdd.party.dto.request.CreateDeliveryPartyRequest;
import com.leets.tdd.party.dto.request.UpdateDeliveryPartyRequest;
import com.leets.tdd.party.dto.response.CreateDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.CompleteDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.CompleteMvpSettlementResponse;
import com.leets.tdd.party.dto.response.CloseDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.DeliveryPartyDetailResponse;
import com.leets.tdd.party.dto.response.DeliveryPartySearchItemResponse;
import com.leets.tdd.party.dto.response.DeliveryPartySearchResponse;
import com.leets.tdd.party.dto.response.LeaveDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.JoinDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.MyDeliveryPartyListResponse;
import com.leets.tdd.party.dto.response.MyDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.OrderDeliveryPartyResponse;
import com.leets.tdd.party.dto.response.PartyParticipantListResponse;
import com.leets.tdd.party.dto.response.PartyParticipantResponse;
import com.leets.tdd.party.dto.response.RecruitingDeliveryPartyListResponse;
import com.leets.tdd.party.dto.response.RecruitingDeliveryPartyResponse;
import com.leets.tdd.party.exception.PartyErrorCode;
import com.leets.tdd.party.exception.PartyException;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.FoodCategoryRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryPartyService {

    private final DeliveryPartyRepository deliveryPartyRepository;
    private final UserRepository userRepository;
    private final FoodCategoryRepository foodCategoryRepository;
    private final PartyParticipantRepository partyParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatService chatService;
    private final ImageStorageService imageStorageService;


    // 배달팟 생성 API
    @Transactional
    public CreateDeliveryPartyResponse createDeliveryParty(
            CreateDeliveryPartyRequest request,
            Long currentUserId
    ) {

        DeliveryParty deliveryParty = new DeliveryParty(
                currentUserId,
                request.getFoodCategoryId(),
                request.getDormitory(),
                request.getTitle(),
                request.getDescription(),
                request.getMinParticipants(),
                request.getMaxParticipants(),
                request.getOrderExpectedAt(),
                PartyStatus.RECRUITING,
                null,
                SettlementStatus.NONE,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        DeliveryParty savedDeliveryParty = deliveryPartyRepository.save(deliveryParty);
        partyParticipantRepository.save(new PartyParticipant(
                savedDeliveryParty.getId(),
                currentUserId,
                PartyParticipantRole.HOST,
                PartyParticipantStatus.JOINED,
                LocalDateTime.now()
        ));

        return new CreateDeliveryPartyResponse(
                savedDeliveryParty.getId(),
                savedDeliveryParty.getFoodCategoryId(),
                savedDeliveryParty.getDormitory(),
                savedDeliveryParty.getTitle(),
                savedDeliveryParty.getDescription(),
                savedDeliveryParty.getMinParticipants(),
                savedDeliveryParty.getMaxParticipants(),
                savedDeliveryParty.getOrderExpectedAt(),
                savedDeliveryParty.getStatus().name(),
                savedDeliveryParty.getCreatedAt()
        );
    }


    // 배달팟 목록 조회 API
    public List<CreateDeliveryPartyResponse> getDeliveryParties() {

        List<DeliveryParty> deliveryParties =
                deliveryPartyRepository.findAllByOrderByCreatedAtDesc();

        return deliveryParties.stream()
                .map(deliveryParty -> new CreateDeliveryPartyResponse(
                        deliveryParty.getId(),
                        deliveryParty.getFoodCategoryId(),
                        deliveryParty.getDormitory(),
                        deliveryParty.getTitle(),
                        deliveryParty.getDescription(),
                        deliveryParty.getMinParticipants(),
                        deliveryParty.getMaxParticipants(),
                        deliveryParty.getOrderExpectedAt(),
                        deliveryParty.getStatus().name(),
                        deliveryParty.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public RecruitingDeliveryPartyListResponse getRecruitingDeliveryParties(
            Long categoryId,
            String dormitory,
            LocalDateTime orderExpectedFrom,
            LocalDateTime orderExpectedTo
    ) {
        List<RecruitingDeliveryPartyResponse> parties = deliveryPartyRepository
                .findRecruitingDeliveryParties(
                        categoryId,
                        normalizeDormitory(dormitory),
                        orderExpectedFrom,
                        orderExpectedTo
                )
                .stream()
                .map(party -> new RecruitingDeliveryPartyResponse(
                        party.getPartyId(),
                        party.getTitle(),
                        party.getCategory(),
                        Math.toIntExact(party.getCurrentParticipants()),
                        party.getMinParticipants(),
                        party.getMaxParticipants(),
                        party.getStatus(),
                        party.getOrderExpectedAt(),
                        party.getDormitory()
                ))
                .toList();

        return new RecruitingDeliveryPartyListResponse(parties);
    }


    // 배달팟 상세 조회 API
    public DeliveryPartyDetailResponse getDeliveryPartyDetail(Long partyId) {

        DeliveryParty deliveryParty = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));
        User leader = userRepository.findById(deliveryParty.getCreatorId())
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTICIPANT_LIST_FAILED));
        return new DeliveryPartyDetailResponse(
                deliveryParty,
                leader,
                resolveProfileImageUrl(leader.getProfileImageUrl())
        );
    }

    @Transactional(readOnly = true)
    public PartyParticipantListResponse getPartyParticipants(Long partyId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        List<PartyParticipant> joinedParticipants = partyParticipantRepository
                .findAllByPartyIdAndStatus(partyId, PartyParticipantStatus.JOINED);
        List<Long> userIds = joinedParticipants.stream()
                .map(PartyParticipant::getUserId)
                .toList();

        Map<Long, User> usersById = userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return new PartyParticipantListResponse(
                partyId,
                joinedParticipants.stream()
                        .sorted((left, right) -> left.getRole() == PartyParticipantRole.HOST ? -1
                                : right.getRole() == PartyParticipantRole.HOST ? 1 : 0)
                        .map(participant -> {
            User user = usersById.get(participant.getUserId());
            if (user == null) {
                throw new PartyException(PartyErrorCode.PARTICIPANT_LIST_FAILED);
            }
                            return toParticipantResponse(
                                    user,
                                    participant.getRole() == PartyParticipantRole.HOST ? "OWNER" : "MEMBER"
                            );
                        })
                        .toList()
        );
    }

    private PartyParticipantResponse toParticipantResponse(User user, String role) {
        return new PartyParticipantResponse(
                user.getId(),
                user.getNickname(),
                resolveProfileImageUrl(user.getProfileImageUrl()),
                user.getMannerTemperature(),
                role
        );
    }

    private String resolveProfileImageUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return key;
        }
        return imageStorageService.resolveViewUrl(key);
    }

    @Transactional
    public JoinDeliveryPartyResponse joinDeliveryParty(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.RECRUITMENT_CLOSED);
        }
        if (deliveryParty.getCreatorId().equals(currentUserId)
                || partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
                partyId, currentUserId, PartyParticipantStatus.JOINED)) {
            throw new PartyException(PartyErrorCode.ALREADY_JOINED);
        }

        long currentParticipants = currentParticipants(partyId);
        if (currentParticipants >= deliveryParty.getMaxParticipants()) {
            throw new PartyException(PartyErrorCode.PARTY_FULL);
        }

        try {
            partyParticipantRepository.saveAndFlush(new PartyParticipant(
                    partyId,
                    currentUserId,
                    PartyParticipantRole.MEMBER,
                    PartyParticipantStatus.JOINED,
                    LocalDateTime.now()
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new PartyException(PartyErrorCode.JOIN_FAILED);
        }
        publishNotification(deliveryParty, DeliveryPartyNotificationType.PARTICIPANT_JOINED);

        long updatedParticipants = currentParticipants(partyId);
        if (updatedParticipants >= deliveryParty.getMaxParticipants()) {
            closeParty(deliveryParty, partyId);
        }

        return new JoinDeliveryPartyResponse(
                partyId,
                updatedParticipants,
                deliveryParty.getMaxParticipants()
        );
    }

    @Transactional
    public Long deleteDeliveryParty(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.DELETE_FORBIDDEN);
        }
        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.CANCEL_NOT_RECRUITING);
        }

        deliveryParty.cancel();
        publishNotification(deliveryParty, DeliveryPartyNotificationType.PARTY_CANCELED);
        return partyId;
    }

    @Transactional
    public LeaveDeliveryPartyResponse leaveDeliveryParty(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.HOST_CANNOT_LEAVE);
        }

        PartyParticipant participant = partyParticipantRepository.findByPartyIdAndUserId(partyId, currentUserId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.NOT_PARTICIPANT));
        if (!participant.isJoined()) {
            throw new PartyException(PartyErrorCode.NOT_PARTICIPANT);
        }
        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.LEAVE_NOT_ALLOWED);
        }

        participant.cancel(LocalDateTime.now());
        try {
            partyParticipantRepository.saveAndFlush(participant);
        } catch (DataIntegrityViolationException exception) {
            throw new PartyException(PartyErrorCode.LEAVE_FAILED);
        }
        publishNotification(deliveryParty, DeliveryPartyNotificationType.PARTICIPANT_LEFT);

        return new LeaveDeliveryPartyResponse(
                partyId,
                currentParticipants(partyId),
                deliveryParty.getMaxParticipants()
        );
    }

    private long currentParticipants(Long partyId) {
        return partyParticipantRepository.countByPartyIdAndStatus(
                partyId, PartyParticipantStatus.JOINED
        );
    }

    private void publishNotification(DeliveryParty party, DeliveryPartyNotificationType type) {
        eventPublisher.publishEvent(new DeliveryPartyNotificationEvent(party, type));
    }

    public MyDeliveryPartyListResponse getMyDeliveryParties(
            Long userId,
            MyPartyStatusFilter status,
            Long categoryId,
            String dormitory,
            LocalDateTime orderExpectedFrom,
            LocalDateTime orderExpectedTo
    ) {
        MyPartyStatusFilter filter = status == null ? MyPartyStatusFilter.ALL : status;

        List<MyDeliveryPartyResponse> parties = partyParticipantRepository.findMyDeliveryParties(
                        userId,
                        filter.name(),
                        categoryId,
                        normalizeDormitory(dormitory),
                        orderExpectedFrom,
                        orderExpectedTo
                ).stream()
                .map(party -> new MyDeliveryPartyResponse(
                        party.getPartyId(),
                        party.getTitle(),
                        party.getCategory(),
                        Math.toIntExact(party.getCurrentParticipants()),
                        party.getMinParticipants(),
                        party.getMaxParticipants(),
                        party.getStatus(),
                        party.getOrderExpectedAt(),
                        party.getDormitory()
                ))
                .toList();

        return new MyDeliveryPartyListResponse(parties);
    }

    private String normalizeDormitory(String dormitory) {
        if (dormitory == null || dormitory.isBlank() || "전체".equals(dormitory)) {
            return null;
        }
        return dormitory;
    }

    public DeliveryPartySearchResponse searchDeliveryParties(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new PartyException(PartyErrorCode.SEARCH_KEYWORD_REQUIRED);
        }

        try {
            List<DeliveryParty> deliveryParties = deliveryPartyRepository
                    .findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword.trim());
            if (deliveryParties.isEmpty()) {
                throw new PartyException(PartyErrorCode.SEARCH_RESULT_NOT_FOUND);
            }

            var categoryNames = foodCategoryRepository.findAllById(deliveryParties.stream()
                            .map(DeliveryParty::getFoodCategoryId)
                            .distinct()
                            .toList()).stream()
                    .collect(java.util.stream.Collectors.toMap(category -> category.getId(), category -> category.getName()));

            List<DeliveryPartySearchItemResponse> parties = deliveryParties.stream()
                    .map(party -> new DeliveryPartySearchItemResponse(
                            party.getId(),
                            party.getTitle(),
                            categoryNames.getOrDefault(party.getFoodCategoryId(), "알 수 없음"),
                            Math.toIntExact(partyParticipantRepository.countByPartyIdAndStatus(
                                    party.getId(), PartyParticipantStatus.JOINED
                            )),
                            party.getMaxParticipants(),
                            party.getStatus().name()
                    ))
                    .toList();
            return new DeliveryPartySearchResponse(parties);
        } catch (PartyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PartyException(PartyErrorCode.SEARCH_FAILED);
        }
    }


    // 배달팟 수정 API
    public void updateDeliveryParty(
            Long partyId,
            UpdateDeliveryPartyRequest request,
            Long currentUserId
    ) {

        DeliveryParty deliveryParty = deliveryPartyRepository.findById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));


        // 작성자(파티장)만 수정 가능
        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.NOT_OWNER);
        }

// 모집 중(RECRUITING) 상태에서만 수정 가능
        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.INVALID_PARTY_STATUS);
        }


        deliveryParty.update(
                request.getTitle(),
                request.getDescription(),
                request.getMaxParticipants(),
                request.getOrderExpectedAt()
        );
        deliveryPartyRepository.save(deliveryParty);
    }


    // 배달 완료 API
    @Transactional
    public CompleteDeliveryPartyResponse completeDelivery(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.COMPLETE_FORBIDDEN);
        }

        if (deliveryParty.getStatus() == PartyStatus.DELIVERED) {
            throw new PartyException(PartyErrorCode.ALREADY_DELIVERED);
        }

        if (deliveryParty.getStatus() != PartyStatus.ORDERED) {
            throw new PartyException(PartyErrorCode.COMPLETE_NOT_ORDERED);
        }

        deliveryParty.completeDelivery();
        chatService.sendSystemMessage(partyId, MessageType.DELIVERY_ARRIVED, "배달이 도착했어요!");
        publishNotification(deliveryParty, DeliveryPartyNotificationType.DELIVERY_COMPLETED);

        return new CompleteDeliveryPartyResponse(
                deliveryParty.getId(),
                deliveryParty.getStatus().name()
        );
    }

    @Transactional
    public CloseDeliveryPartyResponse closeDeliveryParty(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));
        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.CLOSE_FORBIDDEN);
        }
        if (deliveryParty.getStatus() != PartyStatus.RECRUITING) {
            throw new PartyException(PartyErrorCode.ALREADY_CLOSED);
        }
        if (currentParticipants(partyId) < deliveryParty.getMinParticipants()) {
            throw new PartyException(PartyErrorCode.CLOSE_MIN_PARTICIPANTS);
        }
        closeParty(deliveryParty, partyId);
        return new CloseDeliveryPartyResponse(
                deliveryParty.getId() == null ? partyId : deliveryParty.getId(),
                deliveryParty.getStatus().name()
        );
    }

    private void closeParty(DeliveryParty deliveryParty, Long partyId) {
        deliveryParty.close();
        chatService.createChatRoom(partyId);
        publishNotification(deliveryParty, DeliveryPartyNotificationType.RECRUITMENT_CLOSED);
    }

    @Transactional
    public OrderDeliveryPartyResponse completeOrder(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.ORDER_FORBIDDEN);
        }

        if (deliveryParty.getStatus() == PartyStatus.ORDERED) {
            throw new PartyException(PartyErrorCode.ALREADY_ORDERED);
        }

        if (deliveryParty.getStatus() != PartyStatus.CLOSED) {
            throw new PartyException(PartyErrorCode.ORDER_NOT_CLOSED);
        }

        deliveryParty.completeOrder();
        chatService.sendSystemMessage(partyId, MessageType.ORDER_COMPLETED, "주문이 완료됐어요!");
        publishNotification(deliveryParty, DeliveryPartyNotificationType.ORDER_COMPLETED);

        return new OrderDeliveryPartyResponse(
                deliveryParty.getId(),
                deliveryParty.getStatus().name(),
                deliveryParty.getSettlementStatus().name()
        );
    }

    @Transactional
    public CompleteMvpSettlementResponse completeMvpSettlement(Long partyId, Long currentUserId) {
        DeliveryParty deliveryParty = deliveryPartyRepository.findWithLockById(partyId)
                .orElseThrow(() -> new PartyException(PartyErrorCode.PARTY_NOT_FOUND));

        if (!deliveryParty.getCreatorId().equals(currentUserId)) {
            throw new PartyException(PartyErrorCode.SETTLEMENT_FORBIDDEN);
        }
        if (deliveryParty.getStatus() == PartyStatus.SETTLED) {
            throw new PartyException(PartyErrorCode.ALREADY_SETTLED);
        }
        if (deliveryParty.getStatus() != PartyStatus.DELIVERED) {
            throw new PartyException(PartyErrorCode.SETTLEMENT_NOT_DELIVERED);
        }

        deliveryParty.settleForMvp();
        chatService.sendSystemMessage(partyId, MessageType.REVIEW_REQUEST, "정산이 완료됐어요! 배달팟은 어떠셨나요? 후기를 남겨주세요");
        return new CompleteMvpSettlementResponse(deliveryParty.getId(), deliveryParty.getStatus().name());
    }
}
