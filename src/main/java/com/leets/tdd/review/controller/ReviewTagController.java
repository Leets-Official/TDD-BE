package com.leets.tdd.review.controller;

import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.review.dto.ReviewTagListResponse;
import com.leets.tdd.review.service.ReviewTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/review-tags")
public class ReviewTagController {

  private final ReviewTagService reviewTagService;

  @GetMapping
  public ResponseEntity<ApiResponse<ReviewTagListResponse>> getReviewTags() {
    return ResponseEntity.ok(ApiResponse.success("평가 태그 목록을 조회했습니다.", reviewTagService.getReviewTags()));
  }
}
