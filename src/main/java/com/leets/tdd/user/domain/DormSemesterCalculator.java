package com.leets.tdd.user.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 기숙사 인증은 학기 단위로 유지된다(FR-AUTH-02). 인증 시점이 속한 학기의 마지막 순간을
 * 고정 날짜로 계산한다 - 승인일로부터 며칠 뒤가 아니라, 그 학기가 끝나는 날짜에 맞춰 만료된다.
 * <p>
 * 3/1~8/31 인증 -&gt; 그 해 8/31 23:59:59.
 * 9/1~(다음 해) 2/28(29) 인증 -&gt; 그 학기가 끝나는 해의 2월 마지막날 23:59:59
 * (9~12월에 인증했으면 다음 해 2월, 1~2월에 인증했으면 그 해 2월 - 둘 다 같은 학기에 속하기 때문).
 */
public final class DormSemesterCalculator {

    private DormSemesterCalculator() {
    }

    public static LocalDateTime calcSemesterEnd(LocalDateTime verifiedAt) {
        int month = verifiedAt.getMonthValue();
        int year = verifiedAt.getYear();

        if (month >= 3 && month <= 8) {
            return LocalDateTime.of(year, 8, 31, 23, 59, 59);
        }

        // 9~12월에 인증했으면 학기가 다음 해 2월에 끝나고, 1~2월에 인증했으면 이미 그 학기
        // 안에 있는 것이므로 같은 해 2월에 끝난다.
        int febYear = (month >= 9) ? year + 1 : year;
        int lastDayOfFeb = LocalDate.of(febYear, 2, 1).lengthOfMonth();
        return LocalDateTime.of(febYear, 2, lastDayOfFeb, 23, 59, 59);
    }
}
