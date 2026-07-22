package com.leets.tdd.user.service;

import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.user.domain.DormStatus;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.exception.UserErrorCode;
import com.leets.tdd.user.exception.UserException;
import com.leets.tdd.user.repository.DormitoryRepository;
import com.leets.tdd.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DormitoryRepository dormitoryRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserService userService;

    private User newUser() {
        return new User("abcd@gachon.ac.kr", "가나디", "encoded-pw",
                "refresh-hash", LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("Authorization 헤더가 없거나 형식이 안 맞으면 예외가 발생한다")
    void getMyPage_invalidToken() {
        when(jwtProvider.resolveToken(null)).thenReturn(null);

        assertThatThrownBy(() -> userService.getMyPage(null))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰이 만료됐으면 예외가 발생한다")
    void getMyPage_expiredToken() {
        when(jwtProvider.resolveToken("Bearer expired")).thenReturn("expired");
        when(jwtProvider.parseUserId("expired")).thenThrow(mock(ExpiredJwtException.class));

        assertThatThrownBy(() -> userService.getMyPage("Bearer expired"))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.TOKEN_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("서명이 잘못된 토큰이면 예외가 발생한다")
    void getMyPage_invalidSignature() {
        when(jwtProvider.resolveToken("Bearer bad")).thenReturn("bad");
        when(jwtProvider.parseUserId("bad")).thenThrow(mock(SignatureException.class));

        assertThatThrownBy(() -> userService.getMyPage("Bearer bad"))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰은 유효한데 유저가 없으면 예외가 발생한다")
    void getMyPage_userNotFound() {
        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyPage("Bearer valid"))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("기숙사 인증 정보가 없으면(미인증) 관련 필드가 전부 null로 내려간다")
    void getMyPage_noDormitory() {
        User user = newUser();
        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());

        MyPageResponse response = userService.getMyPage("Bearer valid");

        assertThat(response.nickname()).isEqualTo("가나디");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.dormitory()).isNull();
        assertThat(response.dormStatus()).isNull();
        assertThat(response.dormVerifiedAt()).isNull();
        assertThat(response.dormVerifiedUntil()).isNull();
    }

    @Test
    @DisplayName("기숙사 인증 정보가 있으면 그대로 응답에 포함된다")
    void getMyPage_withDormitory() {
        User user = newUser();
        Dormitory dormitory = new Dormitory(1L, "1동", "s3-key");
        dormitory.approve(LocalDateTime.now().plusMonths(4));

        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        MyPageResponse response = userService.getMyPage("Bearer valid");

        assertThat(response.dormitory()).isEqualTo("1동");
        assertThat(response.dormStatus()).isEqualTo(DormStatus.APPROVED.name());
        assertThat(response.dormVerifiedUntil()).isNotNull();
    }

    @Test
    @DisplayName("기숙사 인증이 거절됐으면 거절 사유가 응답에 포함된다")
    void getMyPage_dormitoryRejected() {
        User user = newUser();
        Dormitory dormitory = new Dormitory(1L, "1동", "s3-key");
        dormitory.reject("사진이 흐릿합니다");

        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        MyPageResponse response = userService.getMyPage("Bearer valid");

        assertThat(response.dormStatus()).isEqualTo(DormStatus.REJECTED.name());
        assertThat(response.rejectReason()).isEqualTo("사진이 흐릿합니다");
    }

    @Test
    @DisplayName("동만 입력하고 사진을 제출하지 않았으면 NOT_SUBMITTED로 내려간다")
    void getMyPage_dormitoryNotSubmitted() {
        User user = newUser();
        Dormitory dormitory = new Dormitory(1L, "1동", null);

        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        MyPageResponse response = userService.getMyPage("Bearer valid");

        assertThat(response.dormitory()).isEqualTo("1동");
        assertThat(response.dormStatus()).isEqualTo(DormStatus.NOT_SUBMITTED.name());
        assertThat(response.dormVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("정지 기간이 지났으면 조회 시 자동으로 ACTIVE 상태가 된다")
    void getMyPage_liftsExpiredSuspension() {
        User user = newUser();
        user.suspend(LocalDateTime.now().minusDays(1));

        when(jwtProvider.resolveToken("Bearer valid")).thenReturn("valid");
        when(jwtProvider.parseUserId("valid")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());

        MyPageResponse response = userService.getMyPage("Bearer valid");

        assertThat(response.status()).isEqualTo("ACTIVE");
    }
}
