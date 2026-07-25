package com.leets.tdd.global.config;

import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.global.auth.JwtAuthErrorType;
import com.leets.tdd.global.auth.UserPrincipal;
import com.leets.tdd.user.domain.UserStatus;
import com.leets.tdd.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이고_ACTIVE_계정이면_SecurityContext에_userId가_채워진다() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid");
        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.ACTIVE));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new UserPrincipal(1L));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰이_없으면_SecurityContext를_건드리지_않고_다음_필터로_넘긴다() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(jwtProvider.resolveToken(null)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰이_유효하지_않으면_SecurityContext를_비우고_다음_필터로_넘긴다() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad");
        when(jwtProvider.resolveToken("Bearer bad")).thenReturn("bad");
        when(jwtProvider.parseUserId("bad")).thenThrow(new JwtException("invalid"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 탈퇴한_계정이면_토큰_서명이_유효해도_인증되지_않는다() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid");
        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.DELETED));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(request).setAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE, JwtAuthErrorType.INVALID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 제한된_계정이면_토큰_서명이_유효해도_인증되지_않고_BANNED로_표시된다() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid");
        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.BANNED));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(request).setAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE, JwtAuthErrorType.BANNED);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰의_userId에_해당하는_유저가_DB에_없으면_인증되지_않는다() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid");
        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(request).setAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE, JwtAuthErrorType.INVALID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void SUSPENDED_계정은_정상적으로_인증된다() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid");
        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.SUSPENDED));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(new UserPrincipal(1L));
        verify(filterChain).doFilter(request, response);
    }
}
