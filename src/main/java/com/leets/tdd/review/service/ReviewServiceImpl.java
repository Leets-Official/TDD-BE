package com.leets.tdd.review.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.review.domain.Review;
import com.leets.tdd.review.domain.ReviewTag;
import com.leets.tdd.review.domain.ReviewTagMapping;
import com.leets.tdd.review.dto.CreateReviewRequest;
import com.leets.tdd.review.dto.CreateReviewResponse;
import com.leets.tdd.review.dto.ReceivedReviewListResponse;
import com.leets.tdd.review.dto.ReceivedReviewResponse;
import com.leets.tdd.review.dto.ReviewTargetListResponse;
import com.leets.tdd.review.dto.ReviewTargetResponse;
import com.leets.tdd.review.exception.ReviewErrorCode;
import com.leets.tdd.review.exception.ReviewException;
import com.leets.tdd.review.repository.ReviewRepository;
import com.leets.tdd.review.repository.ReviewTagMappingRepository;
import com.leets.tdd.review.repository.ReviewTagRepository;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
/**
 * 후기 도메인의 권한 검사와 저장 규칙을 한곳에서 처리합니다.
 * 컨트롤러는 HTTP 입출력만, 이 서비스는 비즈니스 규칙만 담당합니다.
 */
public class ReviewServiceImpl implements ReviewService {

  private final DeliveryPartyRepository deliveryPartyRepository;
  private final PartyParticipantRepository partyParticipantRepository;
  private final UserRepository userRepository;
  private final ReviewRepository reviewRepository;
  private final ReviewTagRepository reviewTagRepository;
  private final ReviewTagMappingRepository reviewTagMappingRepository;

  @Override
  public ReviewTargetListResponse getReviewTargets(Long currentUserId, Long partyId) {
    // 팟 존재 여부 → 참여 여부 → 팟 완료 여부 순으로 검사해 권한 없는 사용자에게 정보를 노출하지 않습니다.
    DeliveryParty party = getParty(partyId);
    validateParticipant(partyId, currentUserId);
    validateCompleted(party);

    List<PartyParticipant> participants = partyParticipantRepository.findAllByPartyIdAndStatus(
        partyId,
        PartyParticipantStatus.JOINED
    );
    List<Long> targetUserIds = participants.stream()
        .map(PartyParticipant::getUserId)
        .filter(userId -> !userId.equals(currentUserId))
        .toList();
    Map<Long, User> users = usersById(targetUserIds);

    // 본인은 평가 대상에서 제외하고, 이미 쓴 후기는 reviewed=true로 표시합니다.
    List<ReviewTargetResponse> targets = targetUserIds.stream()
        .map(userId -> ReviewTargetResponse.from(
            users.get(userId),
            reviewRepository.existsByPartyIdAndReviewerIdAndRevieweeId(partyId, currentUserId, userId)
        ))
        .toList();
    return new ReviewTargetListResponse(partyId, targets);
  }

  @Override
  @Transactional
  public CreateReviewResponse createReview(Long currentUserId, Long partyId, CreateReviewRequest request) {
    DeliveryParty party = getParty(partyId);
    validateParticipant(partyId, currentUserId);
    validateCompleted(party);
    validateReviewee(partyId, currentUserId, request.revieweeId());

    if (reviewRepository.existsByPartyIdAndReviewerIdAndRevieweeId(partyId, currentUserId, request.revieweeId())) {
      throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
    }

    List<Long> tagIds = distinctTagIds(request.tagIds());
    validateReviewTags(tagIds);

    try {
      Review review = reviewRepository.saveAndFlush(
          Review.create(partyId, currentUserId, request.revieweeId(), request.rating(), request.content())
      );
      reviewTagMappingRepository.saveAll(tagIds.stream()
          .map(tagId -> ReviewTagMapping.create(review.getId(), tagId))
          .toList());
      User reviewee = userRepository.findById(request.revieweeId())
          .orElseThrow(() -> new ReviewException(ReviewErrorCode.REVIEWEE_NOT_PARTICIPANT));
      reviewee.updateMannerTemperature(mannerTemperatureDelta(request.rating()));
      return CreateReviewResponse.from(review, tagIds);
    } catch (DataIntegrityViolationException exception) {
      // 서비스 단의 사전 검사 사이에 동시에 같은 후기가 저장될 수 있어 DB 유니크 제약도 함께 방어합니다.
      throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
    }
  }

  @Override
  public ReceivedReviewListResponse getReceivedReviews(Long currentUserId, int page, int size) {
    // 작성자 정보는 응답에 포함하지 않아 후기 익명성을 보장합니다.
    Page<Review> reviews = reviewRepository.findByRevieweeId(
        currentUserId,
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
    );
    List<Review> items = reviews.getContent();
    Map<Long, DeliveryParty> parties = partiesById(items.stream().map(Review::getPartyId).toList());
    Map<Long, List<Long>> tagIdsByReviewId = tagIdsByReviewId(items.stream().map(Review::getId).toList());
    Map<Long, String> labelsByTagId = labelsByTagId(tagIdsByReviewId.values().stream().flatMap(Collection::stream).toList());

    List<ReceivedReviewResponse> responses = items.stream()
        .map(review -> ReceivedReviewResponse.from(
            review,
            parties.get(review.getPartyId()).getTitle(),
            tagIdsByReviewId.getOrDefault(review.getId(), List.of()).stream()
                .map(labelsByTagId::get)
                .toList()
        ))
        .toList();

    return new ReceivedReviewListResponse(
        responses,
        reviews.getNumber(),
        reviews.getSize(),
        reviews.getTotalElements(),
        reviews.getTotalPages(),
        reviews.hasNext()
    );
  }

  private DeliveryParty getParty(Long partyId) {
    return deliveryPartyRepository.findById(partyId)
        .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTY_NOT_FOUND));
  }

  private void validateParticipant(Long partyId, Long currentUserId) {
    if (!partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
        partyId,
        currentUserId,
        PartyParticipantStatus.JOINED
    )) {
      throw new ReviewException(ReviewErrorCode.NOT_PARTICIPANT);
    }
  }

  private void validateCompleted(DeliveryParty party) {
    if (party.getStatus() != PartyStatus.COMPLETED) {
      throw new ReviewException(ReviewErrorCode.PARTY_NOT_COMPLETED);
    }
  }

  private void validateReviewee(Long partyId, Long currentUserId, Long revieweeId) {
    if (currentUserId.equals(revieweeId)) {
      throw new ReviewException(ReviewErrorCode.SELF_REVIEW_NOT_ALLOWED);
    }
    if (!partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
        partyId,
        revieweeId,
        PartyParticipantStatus.JOINED
    )) {
      throw new ReviewException(ReviewErrorCode.REVIEWEE_NOT_PARTICIPANT);
    }
  }

  private List<Long> distinctTagIds(List<Long> tagIds) {
    if (tagIds == null) {
      return List.of();
    }
    // LinkedHashSet은 중복을 제거하면서 요청에서 전달된 태그 순서는 보존합니다.
    return new ArrayList<>(new LinkedHashSet<>(tagIds));
  }

  private void validateReviewTags(List<Long> tagIds) {
    if (tagIds.isEmpty()) {
      return;
    }
    if (reviewTagRepository.findAllById(tagIds).size() != tagIds.size()) {
      throw new ReviewException(ReviewErrorCode.REVIEW_TAG_NOT_FOUND);
    }
  }

  private BigDecimal mannerTemperatureDelta(Integer rating) {
    return switch (rating) {
      case 5 -> new BigDecimal("0.5");
      case 4 -> new BigDecimal("0.2");
      case 3 -> BigDecimal.ZERO;
      case 2 -> new BigDecimal("-0.2");
      case 1 -> new BigDecimal("-0.5");
      default -> throw new ReviewException(ReviewErrorCode.REVIEW_TAG_NOT_FOUND);
    };
  }

  private Map<Long, User> usersById(List<Long> userIds) {
    return userRepository.findAllByIdIn(userIds).stream()
        .collect(Collectors.toMap(User::getId, user -> user));
  }

  private Map<Long, DeliveryParty> partiesById(List<Long> partyIds) {
    return deliveryPartyRepository.findAllById(new HashSet<>(partyIds)).stream()
        .collect(Collectors.toMap(DeliveryParty::getId, party -> party));
  }

  private Map<Long, List<Long>> tagIdsByReviewId(List<Long> reviewIds) {
    return reviewTagMappingRepository.findAllByReviewIdIn(reviewIds).stream()
        .collect(Collectors.groupingBy(
            ReviewTagMapping::getReviewId,
            Collectors.mapping(ReviewTagMapping::getReviewTagId, Collectors.toList())
        ));
  }

  private Map<Long, String> labelsByTagId(List<Long> tagIds) {
    return reviewTagRepository.findAllById(new HashSet<>(tagIds)).stream()
        .collect(Collectors.toMap(ReviewTag::getId, ReviewTag::getLabel));
  }
}
