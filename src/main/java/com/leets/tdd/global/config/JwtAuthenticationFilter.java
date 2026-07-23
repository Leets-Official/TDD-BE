package com.leets.tdd.global.config;

import com.leets.tdd.auth.jwt.JwtProvider;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization 헤더의 access token(JWT)을 검증해서 SecurityContext에 Authentication을 채워 넣는다.
 * 토큰이 없거나(퍼블릭 엔드포인트) 유효하지 않으면 그냥 다음 필터로 넘긴다 - 실제로 인증을
 * 요구할지 말지는 SecurityConfig의 authorizeHttpRequests가 결정한다(이 필터는 "토큰이 있으면
 * 누구인지 알려주는" 역할만 하고, "인증이 꼭 있어야 한다"는 판단은 하지 않는다).
 * principal은 별도의 UserDetails 없이 userId(Long)를 그대로 사용한다.
 * -> 컨트롤러에서는 @AuthenticationPrincipal Long userId로 바로 받을 수 있다.
 * 주의: 토큰이 없거나 잘못된 경우 여기서 401/403을 직접 만들지 않는다. Spring Security의
 * 필터 체인은 DispatcherServlet 이전 단계라 @RestControllerAdvice가 잡아주지 못하기 때문에,
 * 세밀한 에러 메시지(만료/서명오류 구분 등)가 필요하면 별도의 AuthenticationEntryPoint를
 * 추가해야 한다. 지금은 우선 "인증 안 됨" 상태로만 넘기고 Security의 기본 403으로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = jwtProvider.resolveToken(request.getHeader(HttpHeaders.AUTHORIZATION));

        if (token != null) {
            try {
                Long userId = jwtProvider.parseUserId(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
