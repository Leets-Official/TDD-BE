package com.leets.tdd.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void 매너_온도_변경_시_변화량을_더하고_수정시각을_갱신한다() {
    User user = new User("user@school.ac.kr", "password", "닉네임");

    user.updateMannerTemperature(new BigDecimal("0.5"));

    assertThat(user.getMannerTemperature()).isEqualByComparingTo("3.5");
    assertThat(user.getUpdatedAt()).isNotNull();
  }

  @Test
  void 매너_온도는_10_0을_초과하지_않는다() {
    User user = new User("user@school.ac.kr", "password", "닉네임");

    for (int i = 0; i < 20; i++) {
      user.updateMannerTemperature(new BigDecimal("0.5"));
    }

    assertThat(user.getMannerTemperature()).isEqualByComparingTo("10.0");
  }

  @Test
  void 매너_온도는_0_0_미만으로_내려가지_않는다() {
    User user = new User("user@school.ac.kr", "password", "닉네임");

    for (int i = 0; i < 20; i++) {
      user.updateMannerTemperature(new BigDecimal("-0.5"));
    }

    assertThat(user.getMannerTemperature()).isEqualByComparingTo("0.0");
  }
}
