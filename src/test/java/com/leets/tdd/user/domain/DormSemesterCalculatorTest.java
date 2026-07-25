package com.leets.tdd.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DormSemesterCalculatorTest {

    @Test
    @DisplayName("3/1~8/31 사이에 인증하면 그 해 8/31 23:59:59로 끝난다")
    void calcSemesterEnd_marchToAugust_endsSameYearAugust() {
        assertThat(DormSemesterCalculator.calcSemesterEnd(LocalDateTime.of(2026, 3, 1, 0, 0)))
                .isEqualTo(LocalDateTime.of(2026, 8, 31, 23, 59, 59));

        assertThat(DormSemesterCalculator.calcSemesterEnd(LocalDateTime.of(2026, 8, 31, 23, 59)))
                .isEqualTo(LocalDateTime.of(2026, 8, 31, 23, 59, 59));
    }

    @Test
    @DisplayName("9/1~12/31 사이에 인증하면 다음 해 2월 마지막날 23:59:59로 끝난다")
    void calcSemesterEnd_septemberToDecember_endsNextYearFebruary() {
        assertThat(DormSemesterCalculator.calcSemesterEnd(LocalDateTime.of(2026, 9, 1, 0, 0)))
                .isEqualTo(LocalDateTime.of(2027, 2, 28, 23, 59, 59));

        assertThat(DormSemesterCalculator.calcSemesterEnd(LocalDateTime.of(2026, 12, 31, 23, 59)))
                .isEqualTo(LocalDateTime.of(2027, 2, 28, 23, 59, 59));
    }

    @Test
    @DisplayName("1/1~2월 말 사이에 인증하면 같은 해 2월 마지막날 23:59:59로 끝난다(직전 학기 연장선)")
    void calcSemesterEnd_januaryToFebruary_endsSameYearFebruary() {
        assertThat(DormSemesterCalculator.calcSemesterEnd(LocalDateTime.of(2027, 1, 1, 0, 0)))
                .isEqualTo(LocalDateTime.of(2027, 2, 28, 23, 59, 59));

        assertThat(DormSemesterCalculator.calcSemesterEnd(LocalDateTime.of(2027, 2, 15, 0, 0)))
                .isEqualTo(LocalDateTime.of(2027, 2, 28, 23, 59, 59));
    }

    @Test
    @DisplayName("윤년이면 2월 29일까지로 끝난다")
    void calcSemesterEnd_leapYear_endsFebruary29() {
        // 2028년은 윤년 -> 2027년 9월~12월에 인증하면 2028년 2월 29일까지.
        assertThat(DormSemesterCalculator.calcSemesterEnd(LocalDateTime.of(2027, 9, 1, 0, 0)))
                .isEqualTo(LocalDateTime.of(2028, 2, 29, 23, 59, 59));

        // 2028년 1~2월에 인증해도 같은 학기(2028년 2월)라 2월 29일까지.
        assertThat(DormSemesterCalculator.calcSemesterEnd(LocalDateTime.of(2028, 1, 15, 0, 0)))
                .isEqualTo(LocalDateTime.of(2028, 2, 29, 23, 59, 59));
    }
}
