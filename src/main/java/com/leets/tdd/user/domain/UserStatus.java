package com.leets.tdd.user.domain;

/**
 * 사용자 계정 상태.
 * ACTIVE: 정상 이용 가능
 * SUSPENDED: 노쇼 누적(3회/5회)으로 일시 제한. 배달 팟 개설/참여만 제한되고 로그인 등 나머지는 정상 이용 가능.
 * BANNED: 노쇼 누적(8회)으로 영구 제한. 로그인 자체가 차단된다. 이 상태에서는 탈퇴(DELETED 전환) 불가.
 * DELETED: 회원 탈퇴(soft delete). 같은 이메일로 재가입 시 이 row를 재사용(reactivate)한다.
 */
public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    BANNED,
    DELETED
}
