package com.leets.tdd.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BankTest {

  @Test
  void 목록에_있는_은행명이면_지원한다() {
    assertThat(Bank.isSupported("카카오뱅크")).isTrue();
  }

  @Test
  void 목록에_없는_은행명이면_지원하지_않는다() {
    assertThat(Bank.isSupported("나만의은행")).isFalse();
  }

  @Test
  void null이면_지원하지_않는다() {
    assertThat(Bank.isSupported(null)).isFalse();
  }
}
