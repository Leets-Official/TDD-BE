package com.leets.tdd.party.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leets.tdd.global.config.SecurityConfig;
import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.party.dto.response.FoodCategoryListResponse;
import com.leets.tdd.party.dto.response.FoodCategoryResponse;
import com.leets.tdd.party.service.FoodCategoryService;
import com.leets.tdd.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FoodCategoryController.class)
@Import(SecurityConfig.class)
class FoodCategoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private FoodCategoryService foodCategoryService;

  @MockitoBean
  private JwtProvider jwtProvider;

  @MockitoBean
  private UserRepository userRepository;

  @Test
  void 토큰_없이_음식_카테고리_목록을_조회한다() throws Exception {
    given(foodCategoryService.getFoodCategories()).willReturn(
        new FoodCategoryListResponse(List.of(
            new FoodCategoryResponse(1L, "한식"),
            new FoodCategoryResponse(2L, "중식")
        ))
    );

    mockMvc.perform(get("/api/v1/food-categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("음식 카테고리 목록 조회에 성공했습니다."))
        .andExpect(jsonPath("$.data.categories[0].foodCategoryId").value(1))
        .andExpect(jsonPath("$.data.categories[0].name").value("한식"));
  }
}
