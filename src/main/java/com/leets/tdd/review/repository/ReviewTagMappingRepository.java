package com.leets.tdd.review.repository;

import com.leets.tdd.review.domain.ReviewTagMapping;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewTagMappingRepository extends JpaRepository<ReviewTagMapping, Long> {

  List<ReviewTagMapping> findAllByReviewIdIn(Collection<Long> reviewIds);
}
