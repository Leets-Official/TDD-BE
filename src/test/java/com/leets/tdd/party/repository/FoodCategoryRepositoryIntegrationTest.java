package com.leets.tdd.party.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.leets.tdd.party.domain.FoodCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
class FoodCategoryRepositoryIntegrationTest {

  @Autowired
  private FoodCategoryRepository foodCategoryRepository;

  @Test
  @Sql(scripts = "/db/migration/V10__seed_food_categories.sql")
  void 초기_카테고리_11개의_ID와_이름을_조회한다() {
    List<FoodCategory> categories = foodCategoryRepository.findAllByOrderByIdAsc();

    assertThat(categories)
        .extracting(FoodCategory::getId, FoodCategory::getName)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(1L, "한식"),
            org.assertj.core.groups.Tuple.tuple(2L, "중식"),
            org.assertj.core.groups.Tuple.tuple(3L, "패스트푸드"),
            org.assertj.core.groups.Tuple.tuple(4L, "카페(디저트)"),
            org.assertj.core.groups.Tuple.tuple(5L, "치킨"),
            org.assertj.core.groups.Tuple.tuple(6L, "분식"),
            org.assertj.core.groups.Tuple.tuple(7L, "일식(회,돈까스)"),
            org.assertj.core.groups.Tuple.tuple(8L, "피자"),
            org.assertj.core.groups.Tuple.tuple(9L, "양식"),
            org.assertj.core.groups.Tuple.tuple(10L, "족발,보쌈"),
            org.assertj.core.groups.Tuple.tuple(11L, "기타")
        );
  }

  @Test
  @Sql(statements = {
      "INSERT INTO food_categories (id, name) VALUES (30, '기타-30')",
      "INSERT INTO food_categories (id, name) VALUES (10, '기타-10')",
      "INSERT INTO food_categories (id, name) VALUES (20, '기타-20')"
  })
  void 카테고리를_ID_오름차순으로_조회한다() {
    List<FoodCategory> categories = foodCategoryRepository.findAllByOrderByIdAsc();

    assertThat(categories).extracting(FoodCategory::getId)
        .containsExactly(10L, 20L, 30L);
  }
}
