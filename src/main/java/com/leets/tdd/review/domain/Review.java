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
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** 한 배달팟에서 한 참여자가 다른 참여자에게 남긴 수정 불가능한 매너 평가입니다. */
public class Review {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "party_id", nullable = false)
  private Long partyId;

  @Column(name = "reviewer_id", nullable = false)
  private Long reviewerId;

  @Column(name = "reviewee_id", nullable = false)
  private Long revieweeId;

  @Column(nullable = false)
  private Integer rating;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public static Review create(Long partyId, Long reviewerId, Long revieweeId, int rating, String content) {
    // 생성 시점만 기록하고 수정 메서드를 제공하지 않아 후기 불변 정책을 유지합니다.
    Review review = new Review();
    review.partyId = partyId;
    review.reviewerId = reviewerId;
    review.revieweeId = revieweeId;
    review.rating = rating;
    review.content = content;
    review.createdAt = LocalDateTime.now();
    return review;
  }
}
