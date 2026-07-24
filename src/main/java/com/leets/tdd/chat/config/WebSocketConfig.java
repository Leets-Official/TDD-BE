package com.leets.tdd.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${client.allowed-origins}")
    private String[] allowedOrigins;

    // 클라이언트가 처음 WebSocket 연결하는 주소
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins);
    }

    // 메시지가 흐르는 통로 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 → 클라이언트 (구독): /topic
        registry.enableSimpleBroker("/topic");
        // 클라이언트 → 서버 (발행): /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}
