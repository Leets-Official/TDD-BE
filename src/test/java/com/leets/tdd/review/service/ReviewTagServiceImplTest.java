package com.leets.tdd.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.leets.tdd.review.domain.ReviewTag;
import com.leets.tdd.review.domain.ReviewTagCategory;
import com.leets.tdd.review.dto.ReviewTagListResponse;
import com.leets.tdd.review.repository.ReviewTagRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewTagServiceImplTest {

  @Mock
  private ReviewTagRepository reviewTagRepository;

  @InjectMocks
  private ReviewTagServiceImpl reviewTagService;

  @Test
  void 평가_태그를_카테고리와_라벨_순으로_조회한다() {
    given(reviewTagRepository.findAllByOrderByCategoryAscLabelAsc()).willReturn(List.of(
        new ReviewTag(ReviewTagCategory.NEGATIVE, "연락이 잘 되지 않아요", null),
        new ReviewTag(ReviewTagCategory.POSITIVE, "응답이 빨라요", null)
    ));

    ReviewTagListResponse response = reviewTagService.getReviewTags();

    assertThat(response.tags()).hasSize(2);
    assertThat(response.tags().get(0).category()).isEqualTo(ReviewTagCategory.NEGATIVE);
    assertThat(response.tags().get(1).label()).isEqualTo("응답이 빨라요");
  }
}
