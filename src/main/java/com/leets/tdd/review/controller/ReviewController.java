package com.leets.tdd.review.controller;

import com.leets.tdd.global.auth.UserPrincipal;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.review.dto.CreateReviewRequest;
import com.leets.tdd.review.dto.CreateReviewResponse;
import com.leets.tdd.review.dto.ReceivedReviewListResponse;
import com.leets.tdd.review.dto.ReviewTargetListResponse;
import com.leets.tdd.review.exception.ReviewErrorCode;
import com.leets.tdd.review.exception.ReviewException;
import com.leets.tdd.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
/**
 * 배달팟 종료 후 진행하는 매너 평가 API를 제공합니다.
 * 인증 필터가 JWT subject로부터 만든 UserPrincipal을 주입합니다.
 */
public class ReviewController {

  private final ReviewService reviewService;

  @GetMapping("/parties/{partyId}/review-targets")
  /** 현재 사용자가 이 팟에서 평가할 수 있는 참여자와 기존 작성 여부를 조회합니다. */
  public ResponseEntity<ApiResponse<ReviewTargetListResponse>> getReviewTargets(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId
  ) {
    ReviewTargetListResponse response = reviewService.getReviewTargets(currentUserId(userPrincipal), partyId);
    return ResponseEntity.ok(ApiResponse.success("평가 대상 목록을 조회했습니다.", response));
  }

  @PostMapping("/parties/{partyId}/reviews")
  /** 팟 참여자가 다른 참여자에게 한 번만 남길 수 있는 매너 평가를 생성합니다. */
  public ResponseEntity<ApiResponse<CreateReviewResponse>> createReview(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @PathVariable Long partyId,
      @Valid @RequestBody CreateReviewRequest request
  ) {
    CreateReviewResponse response = reviewService.createReview(currentUserId(userPrincipal), partyId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("매너 평가가 등록되었습니다.", response));
  }

  @GetMapping("/users/me/reviews")
  /** 작성자 정보는 숨긴 채 현재 사용자가 받은 후기만 최신순으로 반환합니다. */
  public ResponseEntity<ApiResponse<ReceivedReviewListResponse>> getReceivedReviews(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
  ) {
    ReceivedReviewListResponse response = reviewService.getReceivedReviews(currentUserId(userPrincipal), page, size);
    return ResponseEntity.ok(ApiResponse.success("받은 매너 평가를 조회했습니다.", response));
  }

  private Long currentUserId(UserPrincipal userPrincipal) {
    // 인증 필터가 아직 연결되지 않았거나 잘못된 principal을 만든 경우를 명확히 구분합니다.
    if (userPrincipal == null || userPrincipal.userId() == null) {
      throw new ReviewException(ReviewErrorCode.UNAUTHORIZED);
    }
    return userPrincipal.userId();
  }
}
