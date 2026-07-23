package com.leets.tdd.review.service;

import com.leets.tdd.review.dto.ReviewTagListResponse;
import com.leets.tdd.review.dto.ReviewTagResponse;
import com.leets.tdd.review.repository.ReviewTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewTagServiceImpl implements ReviewTagService {

  private final ReviewTagRepository reviewTagRepository;

  @Override
  public ReviewTagListResponse getReviewTags() {
    return new ReviewTagListResponse(
        reviewTagRepository.findAllByOrderByCategoryAscLabelAsc().stream()
            .map(ReviewTagResponse::from)
            .toList()
    );
  }

}
