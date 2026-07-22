package com.leets.tdd.user.service;

import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.exception.UserErrorCode;
import com.leets.tdd.user.exception.UserException;
import com.leets.tdd.user.repository.DormitoryRepository;
import com.leets.tdd.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이슈: 마이페이지 조회 API.
 * 시나리오: Authorization 헤더의 access token 검증 -> 정지기간 지났으면 lazy하게 ACTIVE로 복귀
 * -> User/Dormitory 조회해서 응답 조립.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DormitoryRepository dormitoryRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public MyPageResponse getMyPage(String authorizationHeader) {
        Long userId = extractUserId(authorizationHeader);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (user.isSuspensionExpired()) {
            user.liftSuspension();
        }

        Dormitory dormitory = dormitoryRepository.findByUserId(userId).orElse(null);

        return toMyPageResponse(user, dormitory);
    }

    private Long extractUserId(String authorizationHeader) {
        String token = jwtProvider.resolveToken(authorizationHeader);
        if (token == null) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }
        try {
            return jwtProvider.parseUserId(token);
        } catch (ExpiredJwtException e) {
            throw new UserException(UserErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }
    }

    private MyPageResponse toMyPageResponse(User user, Dormitory dormitory) {
        return new MyPageResponse(
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getMannerTemperature(),
                user.getNoShowApprovedCount(),
                user.getSuspendedUntil(),
                user.getStatus().name(),
                dormitory != null ? dormitory.getDormitory() : null,
                dormitory != null ? dormitory.getDormStatus().name() : null,
                dormitory != null ? dormitory.getDormVerifiedAt() : null,
                dormitory != null ? dormitory.getDormVerifiedUntil() : null,
                dormitory != null ? dormitory.getRejectReason() : null
        );
    }
}
