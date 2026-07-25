package com.leets.tdd.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
            "/api/v1/auth/login",
            "/api/v1/auth/reissue",
            "/api/v1/auth/password-reset",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/delivery-parties/**",
            "/ws/**",
            // 데이터를 조회/변경하지 않는 순수 계산기(운영자용 보조 도구)라 인증 없이 연다.
            "/api/v1/internal/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtProvider jwtProvider,
            ObjectMapper objectMapper,
            UserRepository userRepository
    ) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider, userRepository);
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        http
                .cors(Customizer.withDefaults())
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