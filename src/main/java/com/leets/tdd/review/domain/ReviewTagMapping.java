package com.leets.tdd.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "review_tag_mappings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** 후기와 선택 태그의 다대다 관계를 표현하는 연결 엔티티입니다. */
public class ReviewTagMapping {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "review_id", nullable = false)
  private Long reviewId;

  @Column(name = "review_tag_id", nullable = false)
  private Long reviewTagId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public static ReviewTagMapping create(Long reviewId, Long reviewTagId) {
    ReviewTagMapping reviewTagMapping = new ReviewTagMapping();
    reviewTagMapping.reviewId = reviewId;
    reviewTagMapping.reviewTagId = reviewTagId;
    reviewTagMapping.createdAt = LocalDateTime.now();
    return reviewTagMapping;
  }
}
