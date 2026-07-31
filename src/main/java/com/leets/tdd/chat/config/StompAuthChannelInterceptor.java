package com.leets.tdd.chat.config;

import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.global.jwt.UserPrincipal;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 프레임의 Authorization 헤더에서 access token을 검증하고,
 * 성공하면 UserPrincipal을 세션에 심는다. 이후 SUBSCRIBE/SEND 등 모든 프레임과
 * @MessageMapping 핸들러에서 principal로 현재 사용자를 꺼낼 수 있다.
 *
 * REST는 JwtAuthenticationFilter가 매 요청마다 검증하지만, WebSocket은 최초
 * CONNECT 한 번만 핸드셰이크하므로 여기서 검증한다. 토큰이 없거나 유효하지 않으면
 * 예외를 던져 연결 자체를 거부한다(REST의 퍼블릭 엔드포인트와 달리 채팅 연결은
 * 인증이 필수).
 *
 * 참고: 발신자 신뢰(senderId) 문제는 해결되지만, "이 사용자가 이 팟 참여자인가"
 * (구독·발행 권한)는 배달팟 join API가 나온 뒤 별도로 검증한다(이슈 #62).
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = jwtProvider.resolveToken(accessor.getFirstNativeHeader("Authorization"));
            if (token == null) {
                throw new JwtException("인증 토큰이 없습니다.");
            }
            // 만료/서명오류/타입불일치면 JwtException 계열이 던져져 CONNECT가 거부된다.
            Long userId = jwtProvider.parseUserId(token);
            accessor.setUser(new UserPrincipal(userId));
        }
        return message;
    }
}
