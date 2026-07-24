package com.leets.tdd.user.service;

import com.leets.tdd.auth.jwt.JwtProvider;
import com.leets.tdd.auth.jwt.RefreshTokenHasher;
import com.leets.tdd.auth.service.EmailVerificationService;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.dto.ProfileRegistrationRequest;
import com.leets.tdd.user.dto.ProfileRegistrationResponse;
import com.leets.tdd.user.dto.WithdrawalRequest;
import com.leets.tdd.user.exception.UserErrorCode;
import com.leets.tdd.user.exception.UserException;
import com.leets.tdd.user.repository.DormitoryRepository;
import com.leets.tdd.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 마이페이지 조회 + 계정등록(회원가입 완료) API.
 * 계정등록은 signup_token 없이 email + email_verification_codes의 verified_at(15분 이내)로
 * 신원을 확인한다. 기존에 탈퇴(DELETED)했던 계정이면 재사용(reactivate)하고,
 * ACTIVE/SUSPENDED면 이미 가입된 이메일로, BANNED거나 정지기간이 안 지난 DELETED면 가입을 막는다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_NICKNAME_GENERATION_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final DormitoryRepository dormitoryRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenHasher refreshTokenHasher;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final NicknameGenerator nicknameGenerator;

    @Transactional
    public MyPageResponse getMyPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (user.isSuspensionExpired()) {
            user.liftSuspension();
        }

        Dormitory dormitory = dormitoryRepository.findByUserId(userId).orElse(null);

        return toMyPageResponse(user, dormitory);
    }

    @Transactional
    public ProfileRegistrationResponse completeSignup(ProfileRegistrationRequest request) {
        String email = request.email();

        // 확인과 소비(삭제)를 하나의 원자적 연산으로 묶어서, 동시에 같은 이메일로 여러 요청이
        // 와도 단 하나만 통과하게 한다(TOCTOU 방지). 이후 로직이 실패하면 @Transactional에 의해
        // 이 소비도 함께 롤백되므로, 같은 인증 기록으로 재시도할 수 있다.
        if (!emailVerificationService.consumeSignupVerification(email)) {
            throw new UserException(UserErrorCode.INVALID_VERIFICATION);
        }

        String nickname = resolveNickname(request.nickname());
        String encodedPassword = passwordEncoder.encode(request.password());

        User user = userRepository.findByEmail(email).orElse(null);
        IssuedTokens tokens;

        if (user == null) {
            user = new User(email, nickname, encodedPassword, "", LocalDateTime.now());
            userRepository.save(user);
            tokens = issueTokens(user.getId());
            user.updateRefreshToken(refreshTokenHasher.hash(tokens.refreshToken()), tokens.refreshTokenExpiresAt());
        } else {
            validateExistingUserForRegistration(user);
            tokens = issueTokens(user.getId());
            user.reactivate(nickname, encodedPassword,
                    refreshTokenHasher.hash(tokens.refreshToken()), tokens.refreshTokenExpiresAt());
        }
        userRepository.save(user);

        applyDormitory(user.getId(), request.dormitory());

        return new ProfileRegistrationResponse(
                user.getNickname(), request.dormitory(), tokens.accessToken(), tokens.refreshToken(), "Bearer");
    }

    /**
     * 계정탈퇴. access token으로 신원은 이미 확인됐으니, 비밀번호 재검증 후 soft delete(status
     * = DELETED) 처리하고 refresh token도 무효화한다(로그아웃과 동일하게 clearRefreshToken()).
     * suspendedUntil/noShowApprovedCount/mannerTemperature는 softDelete()가 건드리지 않으므로
     * 그대로 유지된다(정지 우회 방지 + 재가입 시 이력 복원).
     * <p>
     * TODO: "진행 중인 배달팟(정산 미완료) 여부 확인" 단계는 의도적으로 뺐다. party 도메인
     * 엔티티(DeliveryParty/PartyParticipant) 자체는 이미 있지만, 이 기능을 넣으려면 user 도메인이
     * party/settlement 도메인에 의존하게 돼서 이 PR(로그인/로그아웃/탈퇴) 범위를 벗어난다.
     * 별도 이슈로 분리해서 처리한다(명세 실패 케이스: "진행 중인 배달팟이 있어 탈퇴할 수 없습니다.").
     */
    @Transactional
    public void withdraw(Long userId, WithdrawalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (!user.canWithdraw()) {
            throw new UserException(UserErrorCode.WITHDRAWAL_NOT_ALLOWED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserException(UserErrorCode.PASSWORD_MISMATCH);
        }

        user.softDelete();
        user.clearRefreshToken();
        userRepository.save(user);
    }

    private record IssuedTokens(String accessToken, String refreshToken, LocalDateTime refreshTokenExpiresAt) {
    }

    private IssuedTokens issueTokens(Long userId) {
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);
        LocalDateTime refreshTokenExpiresAt = LocalDateTime.now().plus(jwtProvider.getRefreshTokenValidity());
        return new IssuedTokens(accessToken, refreshToken, refreshTokenExpiresAt);
    }

    /**
     * 기존 회원(email로 찾은) 상태별 가입 가능 여부.
     * ACTIVE/SUSPENDED: 이미 쓰고 있는 계정 -> 이미 가입된 이메일
     * BANNED: 영구 제한 -> 가입 불가
     * DELETED: 탈퇴했던 계정. 정지기간이 아직 안 지났으면(탈퇴로 정지 우회 방지) 가입 불가,
     *          지났으면 재사용(reactivate) 대상으로 통과시킨다.
     */
    private void validateExistingUserForRegistration(User user) {
        switch (user.getStatus()) {
            case ACTIVE, SUSPENDED -> throw new UserException(UserErrorCode.ALREADY_REGISTERED_EMAIL);
            case BANNED -> throw new UserException(UserErrorCode.REGISTRATION_BLOCKED);
            case DELETED -> {
                if (user.isWithinSuspensionPeriod()) {
                    throw new UserException(UserErrorCode.REGISTRATION_BLOCKED);
                }
            }
        }
    }

    private String resolveNickname(String requestedNickname) {
        if (requestedNickname != null && !requestedNickname.isBlank()) {
            if (userRepository.existsByNickname(requestedNickname)) {
                throw new UserException(UserErrorCode.NICKNAME_DUPLICATE);
            }
            return requestedNickname;
        }
        for (int attempt = 0; attempt < MAX_NICKNAME_GENERATION_ATTEMPTS; attempt++) {
            String candidate = nicknameGenerator.generate();
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new UserException(UserErrorCode.NICKNAME_GENERATION_FAILED);
    }

    private void applyDormitory(Long userId, String dormitory) {
        if (dormitory == null || dormitory.isBlank()) {
            return;
        }
        dormitoryRepository.findByUserId(userId).ifPresentOrElse(
                existing -> existing.resubmit(dormitory, null),
                () -> dormitoryRepository.save(new Dormitory(userId, dormitory, null))
        );
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
