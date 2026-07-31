package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 마이페이지 > 알림 구독 등록 요청. 브라우저 Web Push API의 PushSubscription을 그대로 받는다
 * (subscription.endpoint / subscription.toJSON().keys.p256dh / .auth). FCM 토큰이 아니라
 * 이 세 값(endpoint + p256dh + auth)을 저장하는 이유는 User 엔티티/DB 컬럼(push_endpoint,
 * push_p256dh_key, push_auth_key)이 이미 Web Push 방식으로 설계되어 있기 때문이다.
 * 길이 제한은 User 엔티티 컬럼 길이(endpoint 500자, key 255자)와 맞춘다.
 */
public record PushSubscriptionRequest(

        @NotBlank(message = "endpoint를 입력해주세요.")
        @Size(max = 500, message = "endpoint는 500자 이하로 입력해주세요.")
        String endpoint,

        @NotBlank(message = "p256dhKey를 입력해주세요.")
        @Size(max = 255, message = "p256dhKey는 255자 이하로 입력해주세요.")
        String p256dhKey,

        @NotBlank(message = "authKey를 입력해주세요.")
        @Size(max = 255, message = "authKey는 255자 이하로 입력해주세요.")
        String authKey
) {
}
