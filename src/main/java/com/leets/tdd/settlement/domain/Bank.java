package com.leets.tdd.settlement.domain;

import java.util.Arrays;

/**
 * 계좌 등록 시 선택 가능한 은행 목록(프론트 select 옵션과 1:1 대응).
 * A안(포맷 검증만) 결정에 따라 실명계좌 조회 API 연동 없이 은행명이 이 목록에 있는지만 검증한다.
 * 은행별 계좌번호 자릿수까지는 따로 관리하지 않는다(같은 은행 안에서도 계좌 종류에 따라
 * 자릿수가 달라 정확히 맞추기 어렵고, 그걸 하려면 결국 실명조회 API 수준의 유지보수가 필요해짐).
 * 자릿수는 RegisterBankAccountRequest에서 은행 상관없이 공통 범위로만 체크한다.
 * 은행이 추가/삭제되면 이 enum만 고치면 된다.
 */
public enum Bank {
    KB_KOOKMIN("KB국민은행"),
    SHINHAN("신한은행"),
    WOORI("우리은행"),
    HANA("하나은행"),
    NH_NONGHYEOP("NH농협은행"),
    IBK_GIEOP("IBK기업은행"),
    KAKAO("카카오뱅크"),
    TOSS("토스뱅크"),
    K_BANK("케이뱅크"),
    SC_JEIL("SC제일은행"),
    SAEMAUL("새마을금고"),
    SHINHYEOP("신협"),
    POST("우체국"),
    DAEGU("대구은행"),
    BUSAN("부산은행");

    private final String label;

    Bank(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static boolean isSupported(String label) {
        return Arrays.stream(values()).anyMatch(bank -> bank.label.equals(label));
    }
}
