package com.leets.tdd.auth.service;

import com.leets.tdd.auth.dto.LoginRequest;
import com.leets.tdd.auth.dto.LoginResponse;
import com.leets.tdd.auth.dto.RefreshTokenRequest;
import com.leets.tdd.auth.exception.AuthErrorCode;
import com.leets.tdd.auth.exception.AuthException;
import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.auth.jwt.RefreshTokenHasher;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.domain.UserStatus;
import com.leets.tdd.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 로그인(이메일/비밀번호 -> access/refresh 토큰 발급).
 * 존재하지 않는 이메일/탈퇴한 계정/비밀번호 불일치는 전부 같은 메시지(LOGIN_FAILED)로 응답해서
 * 어떤 이메일이 실제 가입 계정인지 유추할 수 없게 한다(계정 존재 여부 노출 방지).
 * 5분 내 3회 실패하면 15분간 로그인을 막는다(User.recordFailedLogin/isLoginBlocked)../
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenHasher refreshTokenHasher;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null || user.getStatus() == UserStatus.DELETED) {
            throw new AuthException(AuthErrorCode.LOGIN_FAILED);
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new AuthException(AuthErrorCode.ACCOUNT_BANNED);
        }

        if (user.isLoginBlocked()) {
            throw new AuthException(AuthErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            user.recordFailedLogin();
            userRepository.save(user);
            if (user.isLoginBlocked()) {
                throw new AuthException(AuthErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED);
            }
            throw new AuthException(AuthErrorCode.LOGIN_FAILED);
        }

        user.resetLoginAttempts();

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plus(jwtProvider.getRefreshTokenValidity());
        user.updateRefreshToken(refreshTokenHasher.hash(refreshToken), refreshTokenExpiresAt);
        userRepository.save(user);

        return new LoginResponse(accessToken, refreshToken, "Bearer");
    }

    /**
     * 토큰 재발급. Authorization 헤더 없이, body의 refresh token 자체가 신원 증명이다.
     * 1) JWT로서 유효한지(서명/만료) 확인
     * 2) 그 안의 userId로 찾은 유저의 refreshTokenHash와 일치하는지 확인
     *    (로그인/재발급마다 새 refresh token으로 교체(rotate)하기 때문에, 이미 한 번 쓰인
     *    옛날 refresh token은 여기서 걸러진다 - 탈취된 토큰 재사용 방지)
     * 3) 통과하면 access/refresh 토큰을 둘 다 새로 발급하고, DB의 refreshTokenHash도 새 값으로 교체한다.
     */
    @Transactional
    public LoginResponse reissueToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        Long userId;
        try {
            userId = jwtProvider.parseRefreshUserId(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID));

        if (!refreshTokenHasher.hash(refreshToken).equals(user.getRefreshTokenHash())) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (user.getRefreshTokenExpiresAt() == null || LocalDateTime.now().isAfter(user.getRefreshTokenExpiresAt())) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        String newAccessToken = jwtProvider.createAccessToken(user.getId());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId());
        LocalDateTime newRefreshTokenExpiresAt = LocalDateTime.now().plus(jwtProvider.getRefreshTokenValidity());
        user.updateRefreshToken(refreshTokenHasher.hash(newRefreshToken), newRefreshTokenExpiresAt);
        userRepository.save(user);

        return new LoginResponse(newAccessToken, newRefreshToken, "Bearer");
    }
}
