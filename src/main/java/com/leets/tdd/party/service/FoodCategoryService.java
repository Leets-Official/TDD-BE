package com.leets.tdd.party.service;

import com.leets.tdd.party.dto.response.FoodCategoryListResponse;
import com.leets.tdd.party.dto.response.FoodCategoryResponse;
import com.leets.tdd.party.repository.FoodCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodCategoryService {

  private final FoodCategoryRepository foodCategoryRepository;

  public FoodCategoryListResponse getFoodCategories() {
    return new FoodCategoryListResponse(
        foodCategoryRepository.findAllByOrderByIdAsc().stream()
            .map(category -> new FoodCategoryResponse(category.getId(), category.getName()))
            .toList()
    );
  }
}
