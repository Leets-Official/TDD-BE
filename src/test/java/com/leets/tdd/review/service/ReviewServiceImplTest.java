package com.leets.tdd.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.review.domain.Review;
import com.leets.tdd.review.domain.ReviewTag;
import com.leets.tdd.review.domain.ReviewTagCategory;
import com.leets.tdd.review.domain.ReviewTagMapping;
import com.leets.tdd.review.dto.CreateReviewRequest;
import com.leets.tdd.review.dto.CreateReviewResponse;
import com.leets.tdd.review.dto.ReceivedReviewListResponse;
import com.leets.tdd.review.dto.ReviewTargetListResponse;
import com.leets.tdd.review.repository.ReviewRepository;
import com.leets.tdd.review.repository.ReviewTagMappingRepository;
import com.leets.tdd.review.repository.ReviewTagRepository;
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
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

  @Mock
  private DeliveryPartyRepository deliveryPartyRepository;

  @Mock
  private PartyParticipantRepository partyParticipantRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ReviewTagRepository reviewTagRepository;

  @Mock
  private ReviewTagMappingRepository reviewTagMappingRepository;

  @Mock
  private ImageStorageService imageStorageService;

  @InjectMocks
  private ReviewServiceImpl reviewService;

  @Test
  void 완료된_배달팟의_평가_대상을_조회한다() {
    DeliveryParty party = completedParty();
    PartyParticipant currentUser = participant(1L);
    PartyParticipant target = participant(2L);
    User user = user(2L, "야식요정");

    given(deliveryPartyRepository.findById(10L)).willReturn(Optional.of(party));
    given(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(10L, 1L, PartyParticipantStatus.JOINED))
        .willReturn(true);
    given(partyParticipantRepository.findAllByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
        .willReturn(List.of(currentUser, target));
    given(userRepository.findAllByIdIn(List.of(2L))).willReturn(List.of(user));
    given(reviewRepository.findAllByPartyIdAndReviewerId(10L, 1L)).willReturn(List.of());

    ReviewTargetListResponse response = reviewService.getReviewTargets(1L, 10L);

    assertThat(response.partyId()).isEqualTo(10L);
    assertThat(response.targets()).singleElement()
        .extracting("userId", "nickname", "reviewed")
        .containsExactly(2L, "야식요정", false);
    // 대상 수와 무관하게 후기를 한 번에 읽는지 고정한다(대상마다 exists를 던지는 구현으로 되돌아가지 않도록).
    verify(reviewRepository).findAllByPartyIdAndReviewerId(10L, 1L);
    verify(reviewRepository, never()).existsByPartyIdAndReviewerIdAndRevieweeId(anyLong(), anyLong(), anyLong());
  }

  @Test
  void 이미_평가한_대상만_reviewed로_표시한다() {
    DeliveryParty party = completedParty();
    PartyParticipant currentUser = participant(1L);
    PartyParticipant reviewedTarget = participant(2L);
    PartyParticipant notReviewedTarget = participant(3L);
    User reviewedUser = user(2L, "야식요정");
    User notReviewedUser = user(3L, "새벽배송");
    Review alreadyWritten = Review.create(10L, 1L, 2L, 5, "좋았습니다.");

    given(deliveryPartyRepository.findById(10L)).willReturn(Optional.of(party));
    given(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(10L, 1L, PartyParticipantStatus.JOINED))
        .willReturn(true);
    given(partyParticipantRepository.findAllByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
        .willReturn(List.of(currentUser, reviewedTarget, notReviewedTarget));
    given(userRepository.findAllByIdIn(List.of(2L, 3L)))
        .willReturn(List.of(reviewedUser, notReviewedUser));
    given(reviewRepository.findAllByPartyIdAndReviewerId(10L, 1L)).willReturn(List.of(alreadyWritten));

    ReviewTargetListResponse response = reviewService.getReviewTargets(1L, 10L);

    assertThat(response.targets())
        .extracting("userId", "reviewed")
        .containsExactly(tuple(2L, true), tuple(3L, false));
  }

  @Test
  void 프로필_이미지_key를_절대주소로_변환해_내려준다() {
    DeliveryParty party = completedParty();
    PartyParticipant currentUser = participant(1L);
    PartyParticipant withImage = participant(2L);
    PartyParticipant withoutImage = participant(3L);
    User imageUser = user(2L, "야식요정", "profile/2/abc.jpg");
    User noImageUser = user(3L, "새벽배송", null);

    given(deliveryPartyRepository.findById(10L)).willReturn(Optional.of(party));
    given(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(10L, 1L, PartyParticipantStatus.JOINED))
        .willReturn(true);
    given(partyParticipantRepository.findAllByPartyIdAndStatus(10L, PartyParticipantStatus.JOINED))
        .willReturn(List.of(currentUser, withImage, withoutImage));
    given(userRepository.findAllByIdIn(List.of(2L, 3L))).willReturn(List.of(imageUser, noImageUser));
    given(reviewRepository.findAllByPartyIdAndReviewerId(10L, 1L)).willReturn(List.of());
    given(imageStorageService.resolveViewUrl("profile/2/abc.jpg"))
        .willReturn("https://tdd-public.s3.ap-northeast-2.amazonaws.com/profile/2/abc.jpg");

    ReviewTargetListResponse response = reviewService.getReviewTargets(1L, 10L);

    // 사진이 있으면 base URL을 합친 절대주소로, 없으면 null 그대로 내려간다.
    assertThat(response.targets())
        .extracting("userId", "profileImageUrl")
        .containsExactly(
            tuple(2L, "https://tdd-public.s3.ap-northeast-2.amazonaws.com/profile/2/abc.jpg"),
            tuple(3L, null)
        );
  }

  @Test
  void 중복된_태그는_제거해_후기를_저장한다() {
    DeliveryParty party = completedParty();
    ReviewTag tag = new ReviewTag(ReviewTagCategory.POSITIVE, "응답이 빨라요", null);

    given(deliveryPartyRepository.findById(10L)).willReturn(Optional.of(party));
    given(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(10L, 1L, PartyParticipantStatus.JOINED))
        .willReturn(true);
    given(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(10L, 2L, PartyParticipantStatus.JOINED))
        .willReturn(true);
    given(reviewRepository.existsByPartyIdAndReviewerIdAndRevieweeId(10L, 1L, 2L)).willReturn(false);
    given(reviewTagRepository.findAllById(List.of(1L))).willReturn(List.of(tag));
    given(reviewRepository.saveAndFlush(any(Review.class))).willAnswer(invocation -> invocation.getArgument(0));
    User reviewee = new User("reviewee@school.ac.kr", "password", "평가 대상");
    given(userRepository.findById(2L)).willReturn(Optional.of(reviewee));

    CreateReviewResponse response = reviewService.createReview(
        1L,
        10L,
        new CreateReviewRequest(2L, 5, List.of(1L, 1L), "좋았습니다.")
    );

    assertThat(response.revieweeId()).isEqualTo(2L);
    assertThat(response.tagIds()).containsExactly(1L);
    assertThat(reviewee.getMannerTemperature()).isEqualByComparingTo("3.5");
    verify(reviewTagMappingRepository).saveAll(any());
  }

  @Test
  void 받은_후기를_최신순_페이지로_조회한다() {
    Review review = review(7L, 10L, 1L, 2L);
    DeliveryParty party = party(10L, "치킨 같이 시켜요");
    ReviewTagMapping mapping = mapping(7L, 3L);
    ReviewTag tag = reviewTag(3L, "시간 약속을 잘 지켜요");

    given(reviewRepository.findByRevieweeId(any(), any())).willReturn(new PageImpl<>(List.of(review)));
    given(deliveryPartyRepository.findAllById(any())).willReturn(List.of(party));
    given(reviewTagMappingRepository.findAllByReviewIdIn(List.of(7L))).willReturn(List.of(mapping));
    given(reviewTagRepository.findAllById(any())).willReturn(List.of(tag));

    ReceivedReviewListResponse response = reviewService.getReceivedReviews(2L, 0, 10);

    assertThat(response.items()).singleElement()
        .extracting("partyTitle", "rating", "tags")
        .containsExactly("치킨 같이 시켜요", 5, List.of("시간 약속을 잘 지켜요"));
  }

  private DeliveryParty completedParty() {
    DeliveryParty party = org.mockito.Mockito.mock(DeliveryParty.class);
    given(party.getStatus()).willReturn(PartyStatus.DELIVERED);
    return party;
  }

  private DeliveryParty completedParty(Long id, String title) {
    DeliveryParty party = completedParty();
    given(party.getId()).willReturn(id);
    given(party.getTitle()).willReturn(title);
    return party;
  }

  private DeliveryParty party(Long id, String title) {
    DeliveryParty party = org.mockito.Mockito.mock(DeliveryParty.class);
    given(party.getId()).willReturn(id);
    given(party.getTitle()).willReturn(title);
    return party;
  }

  private PartyParticipant participant(Long userId) {
    PartyParticipant participant = org.mockito.Mockito.mock(PartyParticipant.class);
    given(participant.getUserId()).willReturn(userId);
    return participant;
  }

  private User user(Long id, String nickname) {
    return user(id, nickname, null);
  }

  private User user(Long id, String nickname, String profileImageKey) {
    User user = org.mockito.Mockito.mock(User.class);
    given(user.getId()).willReturn(id);
    given(user.getNickname()).willReturn(nickname);
    given(user.getProfileImageUrl()).willReturn(profileImageKey);
    return user;
  }

  private Review review(Long id, Long partyId, Long reviewerId, Long revieweeId) {
    Review review = org.mockito.Mockito.mock(Review.class);
    given(review.getId()).willReturn(id);
    given(review.getPartyId()).willReturn(partyId);
    given(review.getRating()).willReturn(5);
    given(review.getContent()).willReturn("좋았습니다.");
    given(review.getCreatedAt()).willReturn(LocalDateTime.of(2026, 7, 22, 12, 0));
    return review;
  }

  private ReviewTagMapping mapping(Long reviewId, Long tagId) {
    ReviewTagMapping mapping = org.mockito.Mockito.mock(ReviewTagMapping.class);
    given(mapping.getReviewId()).willReturn(reviewId);
    given(mapping.getReviewTagId()).willReturn(tagId);
    return mapping;
  }

  private ReviewTag reviewTag(Long id, String label) {
    ReviewTag tag = org.mockito.Mockito.mock(ReviewTag.class);
    given(tag.getId()).willReturn(id);
    given(tag.getLabel()).willReturn(label);
    return tag;
  }
}
