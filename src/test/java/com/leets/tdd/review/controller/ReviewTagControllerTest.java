package com.leets.tdd.review.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leets.tdd.global.config.SecurityConfig;
import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.review.domain.ReviewTagCategory;
import com.leets.tdd.review.dto.ReviewTagListResponse;
import com.leets.tdd.review.dto.ReviewTagResponse;
import com.leets.tdd.review.service.ReviewTagService;
import com.leets.tdd.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewTagController.class)
@Import(SecurityConfig.class)
class ReviewTagControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ReviewTagService reviewTagService;

  @MockitoBean
  private JwtProvider jwtProvider;

  @MockitoBean
  private UserRepository userRepository;

  @Test
  void 인증_없이_평가_태그_목록을_조회한다() throws Exception {
    given(reviewTagService.getReviewTags()).willReturn(new ReviewTagListResponse(List.of(
        new ReviewTagResponse(1L, ReviewTagCategory.POSITIVE, "시간 약속을 잘 지켜요")
    )));

    mockMvc.perform(get("/api/v1/review-tags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("평가 태그 목록을 조회했습니다."))
        .andExpect(jsonPath("$.data.tags[0].tagId").value(1))
        .andExpect(jsonPath("$.data.tags[0].category").value("POSITIVE"));
  }
}
