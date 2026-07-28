package com.leets.tdd.global.webpush;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 webpush.vapid.* 값을 읽어 담는다.
 * public/private 키는 URL-safe Base64(패딩 없음) 형식의 VAPID 키이고,
 * subject는 VAPID 표준이 요구하는 연락처(mailto: 또는 https: URL)다.
 */
@ConfigurationProperties(prefix = "webpush.vapid")
public record WebPushProperties(
        String publicKey,
        String privateKey,
        String subject
) {
}
