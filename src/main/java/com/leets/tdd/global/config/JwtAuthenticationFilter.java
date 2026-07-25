package com.leets.tdd.global.config;

import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.global.jwt.JwtAuthErrorType;
import com.leets.tdd.global.jwt.UserPrincipal;
import com.leets.tdd.user.domain.UserStatus;
import com.leets.tdd.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization 헤더의 access token(JWT)을 검증해서 SecurityContext에 Authentication을 채워 넣는다.
 * 토큰이 없거나(퍼블릭 엔드포인트) 유효하지 않으면 그냥 다음 필터로 넘긴다 - 실제로 인증을
 * 요구할지 말지는 SecurityConfig의 authorizeHttpRequests가 결정한다(이 필터는 "토큰이 있으면
 * 누구인지 알려주는" 역할만 하고, "인증이 꼭 있어야 한다"는 판단은 하지 않는다).
 * 서명/만료가 유효한 토큰이라도 DB 조회 결과 탈퇴(DELETED)/제한(BANNED) 상태면 인증되지 않은
 * 것으로 취급한다 - access token은 탈퇴/제한 이후에도 만료 전까지(최대 30분) 서명 자체는
 * 계속 유효하기 때문에, 매 요청마다 최신 계정 상태를 DB에서 확인해야 한다.
 * principal은 global.jwt.UserPrincipal(팀 컨벤션 - JWT subject로 만든 사용자 식별자)을 사용한다.
 * -> 컨트롤러에서는 @AuthenticationPrincipal UserPrincipal로 받을 수 있다.
 * 주의: 토큰이 없거나 잘못된 경우 여기서 401 응답 바디를 직접 만들지 않는다. Spring Security의
 * 필터 체인은 DispatcherServlet 이전 단계라 @RestControllerAdvice가 잡아주지 못하기 때문에,
 * 실패 이유(만료/서명오류 등 구분)만 request attribute(JWT_ERROR_ATTRIBUTE)에 남겨두고,
 * 실제 {"success":false,"message":"..."} 형태의 응답은 JwtAuthenticationEntryPoint가 만든다.
 * 일부러 @Component로 등록하지 않는다 - SecurityConfig에서 직접 new해서 쓴다(이유는
 * SecurityConfig의 주석 참고: @WebMvcTest가 대상 컨트롤러와 무관하게 Filter 빈을 전부
 * 끌어오는 문제 때문).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 인증 실패 이유(JwtAuthErrorType)를 담아두는 request attribute 키. */
    public static final String JWT_ERROR_ATTRIBUTE = "jwtAuthError";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

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

                // 토큰 자체는 서명/만료가 유효해도, 발급 이후 탈퇴(DELETED)했거나 이용이 제한(BANNED)된
                // 계정일 수 있다(access token은 최대 30분간 서버 상태 없이 유효하기 때문). 매 요청마다
                // DB의 최신 상태를 확인해서 그런 토큰은 인증되지 않은 것으로 취급한다.
                UserStatus status = userRepository.findStatusById(userId).orElse(null);

                if (status == UserStatus.DELETED || status == null) {
                    SecurityContextHolder.clearContext();
                    request.setAttribute(JWT_ERROR_ATTRIBUTE, JwtAuthErrorType.INVALID);
                } else if (status == UserStatus.BANNED) {
                    SecurityContextHolder.clearContext();
                    request.setAttribute(JWT_ERROR_ATTRIBUTE, JwtAuthErrorType.BANNED);
                } else {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(new UserPrincipal(userId), null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(JWT_ERROR_ATTRIBUTE, JwtAuthErrorType.EXPIRED);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(JWT_ERROR_ATTRIBUTE, JwtAuthErrorType.INVALID);
            }
        } else {
            // Authorization 헤더 자체가 없는 경우. 퍼블릭 엔드포인트일 수도 있어서 여기서 막지는
            // 않고, 인증이 필요한 엔드포인트였다면 JwtAuthenticationEntryPoint가 이 값을 보고
            // "토큰이 없다"는 메시지로 응답한다.
            request.setAttribute(JWT_ERROR_ATTRIBUTE, JwtAuthErrorType.MISSING);
        }

        filterChain.doFilter(request, response);
    }
}
