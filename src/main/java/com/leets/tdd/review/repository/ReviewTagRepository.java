package com.leets.tdd.review.repository;

import com.leets.tdd.review.domain.ReviewTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewTagRepository extends JpaRepository<ReviewTag, Long> {

  List<ReviewTag> findAllByOrderByCategoryAscLabelAsc();

  List<ReviewTag> findAllByIdIn(List<Long> ids);
}
