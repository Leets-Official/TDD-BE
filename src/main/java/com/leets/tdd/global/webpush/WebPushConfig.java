package com.leets.tdd.global.webpush;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * 웹푸시 발송에 쓰는 PushService 빈을 VAPID 키로 초기화한다.
 * 페이로드 암호화(ECDH/HKDF)에 BouncyCastle provider가 필요하므로 등록한다.
 */
@Configuration
@EnableConfigurationProperties(WebPushProperties.class)
public class WebPushConfig {

    @Bean
    public PushService pushService(WebPushProperties properties) throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        return new PushService(
                properties.publicKey(),
                properties.privateKey(),
                properties.subject()
        );
    }
}
