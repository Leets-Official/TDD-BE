package com.leets.tdd.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "review_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/** 운영자가 관리하며 평가 화면에서 선택지로 보여 주는 태그입니다. */
public class ReviewTag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private ReviewTagCategory category;

  @Column(nullable = false, length = 50)
  private String label;

  @Column(length = 255)
  private String content;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public ReviewTag(ReviewTagCategory category, String label, String content) {
    this.category = category;
    this.label = label;
    this.content = content;
    this.createdAt = LocalDateTime.now();
  }
}
