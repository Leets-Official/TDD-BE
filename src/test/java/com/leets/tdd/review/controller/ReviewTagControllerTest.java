package com.leets.tdd.review.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leets.tdd.review.domain.ReviewTagCategory;
import com.leets.tdd.review.dto.ReviewTagListResponse;
import com.leets.tdd.review.dto.ReviewTagResponse;
import com.leets.tdd.review.service.ReviewTagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewTagController.class)
class ReviewTagControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ReviewTagService reviewTagService;

  @Test
  @WithMockUser
  void 평가_태그_목록을_조회한다() throws Exception {
    given(reviewTagService.getReviewTags()).willReturn(new ReviewTagListResponse(List.of(
        new ReviewTagResponse(1L, ReviewTagCategory.POSITIVE, "응답이 빨라요")
    )));

    mockMvc.perform(get("/api/v1/review-tags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("평가 태그 목록을 조회했습니다."))
        .andExpect(jsonPath("$.data.tags[0].tagId").value(1))
        .andExpect(jsonPath("$.data.tags[0].category").value("POSITIVE"));
  }
}
