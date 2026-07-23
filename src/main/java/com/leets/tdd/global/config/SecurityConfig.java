package com.leets.tdd.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leets.tdd.auth.jwt.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/health",
            "/api/v1/auth/email/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtProvider jwtProvider,
            ObjectMapper objectMapper
    ) throws Exception {
        // JwtAuthenticationFilter는 일부러 @Component로 등록하지 않고 여기서 직접 new한다.
        // @Component(Filter)로 등록해두면 @WebMvcTest가 대상 컨트롤러와 무관하게 모든 Filter 빈을
        // 자동으로 끌어오는데, 그러면 SecurityConfig를 import조차 안 한 다른 컨트롤러의 슬라이스
        // 테스트까지 이 필터를 만들려다 JwtProvider 빈을 못 찾아 실패한다(실제로 리뷰 도메인
        // 컨트롤러 테스트에서 이 문제로 컨텍스트 로딩이 깨졌었다). 빈으로 등록하지 않으면
        // 이 SecurityConfig를 실제로 import한 곳에서만 만들어지므로 그 문제가 없다.
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // 계정등록(회원가입 완료)은 아직 로그인 전 상태라 토큰이 없다. GET(마이페이지)은
                        // 인증이 필요하니 이 경로/메서드만 예외로 공개한다.
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/me").permitAll()
                        .anyRequest().authenticated()
                )
                // 토큰이 없거나/만료됐거나/유효하지 않아 인증에 실패하면 Security 기본 403(빈 바디)
                // 대신 API 명세의 {"success":false,"message":"..."} 형식으로 응답한다.
                .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
