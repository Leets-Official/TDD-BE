package com.leets.tdd.party.repository;

import com.leets.tdd.party.domain.FoodCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {

  List<FoodCategory> findAllByOrderByIdAsc();
}
