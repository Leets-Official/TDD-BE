package com.leets.tdd.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 임시 시큐리티 설정.
 * 아직 로그인/JWT 인증이 구현되기 전이라, spring-boot-starter-security 기본값(모든 요청에
 * 로그인 화면 요구)이 API 테스트를 막고 있어서 임시로 전부 열어둠.
 * 실제 로그인/JWT 필터 작업하시는 분이 이 설정을 교체/확장해야 함.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    // 이메일 인증코드를 평문으로 저장하지 않기 위해 사용(EmailVerificationRepository/Service).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
