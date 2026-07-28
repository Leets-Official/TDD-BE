package com.leets.tdd.party.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.leets.tdd.party.domain.FoodCategory;
import com.leets.tdd.party.dto.response.FoodCategoryListResponse;
import com.leets.tdd.party.repository.FoodCategoryRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoodCategoryServiceTest {

  @Mock
  private FoodCategoryRepository foodCategoryRepository;

  @InjectMocks
  private FoodCategoryService foodCategoryService;

  @Test
  void 음식_카테고리를_ID_오름차순으로_조회한다() {
    given(foodCategoryRepository.findAllByOrderByIdAsc())
        .willReturn(List.of(
            new FoodCategory(1L, "한식"),
            new FoodCategory(2L, "중식")
        ));

    FoodCategoryListResponse response = foodCategoryService.getFoodCategories();

    assertThat(response.categories()).extracting("foodCategoryId")
        .containsExactly(1L, 2L);
    assertThat(response.categories()).extracting("name")
        .containsExactly("한식", "중식");
  }
}
