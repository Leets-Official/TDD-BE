package com.leets.tdd.review.repository;

import com.leets.tdd.review.domain.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  /** 같은 팟에서 같은 대상에게 이미 평가를 남겼는지 확인합니다. */
  boolean existsByPartyIdAndReviewerIdAndRevieweeId(Long partyId, Long reviewerId, Long revieweeId);

  List<Review> findAllByPartyIdAndReviewerId(Long partyId, Long reviewerId);

  Page<Review> findByRevieweeId(Long revieweeId, Pageable pageable);
}
