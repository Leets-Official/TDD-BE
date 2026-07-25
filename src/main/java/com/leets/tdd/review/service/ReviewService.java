package com.leets.tdd.review.service;

import com.leets.tdd.review.dto.CreateReviewRequest;
import com.leets.tdd.review.dto.CreateReviewResponse;
import com.leets.tdd.review.dto.ReceivedReviewListResponse;
import com.leets.tdd.review.dto.ReviewTargetListResponse;

public interface ReviewService {

  ReviewTargetListResponse getReviewTargets(Long currentUserId, Long partyId);

  CreateReviewResponse createReview(Long currentUserId, Long partyId, CreateReviewRequest request);

  ReceivedReviewListResponse getReceivedReviews(Long currentUserId, int page, int size);
}
