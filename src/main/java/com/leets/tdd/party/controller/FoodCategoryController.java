package com.leets.tdd.party.controller;

import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.party.dto.response.FoodCategoryListResponse;
import com.leets.tdd.party.service.FoodCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Food Category", description = "음식 카테고리 API")
@RestController
@RequestMapping("/api/v1/food-categories")
@RequiredArgsConstructor
public class FoodCategoryController {

  private final FoodCategoryService foodCategoryService;

  @Operation(summary = "음식 카테고리 목록 조회", description = "배달팟 생성과 필터링에 사용할 음식 카테고리 ID와 이름을 조회합니다.")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "음식 카테고리 목록 조회 성공"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "음식 카테고리 목록 조회 실패")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<FoodCategoryListResponse>> getFoodCategories() {
    return ResponseEntity.ok(ApiResponse.success(
        "음식 카테고리 목록 조회에 성공했습니다.",
        foodCategoryService.getFoodCategories()
    ));
  }
}
