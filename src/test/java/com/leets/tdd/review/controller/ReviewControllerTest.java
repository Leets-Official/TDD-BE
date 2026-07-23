package com.leets.tdd.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leets.tdd.global.auth.UserPrincipal;
import com.leets.tdd.review.dto.CreateReviewResponse;
import com.leets.tdd.review.dto.ReceivedReviewListResponse;
import com.leets.tdd.review.dto.ReceivedReviewResponse;
import com.leets.tdd.review.dto.ReviewTargetListResponse;
import com.leets.tdd.review.dto.ReviewTargetResponse;
import com.leets.tdd.review.service.ReviewService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ReviewService reviewService;

  @Test
  void 평가_대상_목록을_조회한다() throws Exception {
    given(reviewService.getReviewTargets(1L, 10L)).willReturn(new ReviewTargetListResponse(
        10L,
        List.of(new ReviewTargetResponse(2L, "야식요정", null, false))
    ));

    mockMvc.perform(get("/api/v1/parties/10/review-targets").with(user(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.partyId").value(10))
        .andExpect(jsonPath("$.data.targets[0].nickname").value("야식요정"));
  }

  @Test
  void 매너_평가를_등록한다() throws Exception {
    given(reviewService.createReview(any(), any(), any())).willReturn(new CreateReviewResponse(
        7L,
        10L,
        2L,
        5,
        List.of(1L),
        "좋았습니다.",
        LocalDateTime.of(2026, 7, 22, 12, 0)
    ));

    mockMvc.perform(post("/api/v1/parties/10/reviews")
            .with(user(1L))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ReviewRequest(2L, 5, List.of(1L), "좋았습니다."))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.reviewId").value(7));
  }

  @Test
  void 받은_후기를_조회한다() throws Exception {
    given(reviewService.getReceivedReviews(1L, 0, 10)).willReturn(new ReceivedReviewListResponse(
        List.of(new ReceivedReviewResponse(
            7L,
            "치킨 같이 시켜요",
            5,
            List.of("응답이 빨라요"),
            "좋았습니다.",
            LocalDateTime.of(2026, 7, 22, 12, 0)
        )),
        0,
        10,
        1,
        1,
        false
    ));

    mockMvc.perform(get("/api/v1/users/me/reviews").with(user(1L)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].partyTitle").value("치킨 같이 시켜요"))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  private RequestPostProcessor user(Long userId) {
    return authentication(new UsernamePasswordAuthenticationToken(new UserPrincipal(userId), null, List.of()));
  }

  private record ReviewRequest(
      Long revieweeId,
      Integer rating,
      List<Long> tagIds,
      String content
  ) {
  }
}
