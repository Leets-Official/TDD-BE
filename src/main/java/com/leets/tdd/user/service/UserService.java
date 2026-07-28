package com.leets.tdd.user.service;

import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.global.jwt.RefreshTokenHasher;
import com.leets.tdd.global.storage.ImageCategory;
import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
import com.leets.tdd.auth.service.EmailVerificationService;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.dto.DormVerificationConfirmRequest;
import com.leets.tdd.user.dto.DormVerificationPresignRequest;
import com.leets.tdd.user.dto.DormVerificationPresignResponse;
import com.leets.tdd.user.dto.DormVerificationUploadResponse;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.dto.ProfileImageConfirmRequest;
import com.leets.tdd.user.dto.ProfileImagePresignRequest;
import com.leets.tdd.user.dto.ProfileImagePresignResponse;
import com.leets.tdd.user.dto.ProfileImageUploadResponse;
import com.leets.tdd.user.dto.ProfileRegistrationRequest;
import com.leets.tdd.user.dto.ProfileRegistrationResponse;
import com.leets.tdd.user.dto.ChangePasswordRequest;
import com.leets.tdd.user.dto.ProfileUpdateRequest;
import com.leets.tdd.user.dto.ProfileUpdateResponse;
import com.leets.tdd.user.dto.PushSettingRequest;
import com.leets.tdd.user.dto.PushSettingResponse;
import com.leets.tdd.user.dto.PushSubscriptionRequest;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final DeliveryPartyRepository deliveryPartyRepository;
    private final PartyParticipantRepository partyParticipantRepository;
    private final ImageStorageService imageStorageService;

    // 탈퇴 제한: 진행 중인 배달 팟(모집중/마감/주문완료) 또는 완료됐지만 정산이 안 끝난 팟이 있으면 막는다.
    private static final Set<PartyStatus> ONGOING_PARTY_STATUSES =
            EnumSet.of(PartyStatus.RECRUITING, PartyStatus.CLOSED, PartyStatus.ORDERED);
    private static final Set<SettlementStatus> SETTLEMENT_TERMINAL_STATUSES =
            EnumSet.of(SettlementStatus.COMPLETED, SettlementStatus.CANCELED);

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
     * 마이페이지 > 프로필 수정. 닉네임/기숙사 동/프로필 사진을 수정한다.
     * 닉네임이 기존과 같으면(대소문자까지 완전히 동일) 중복 검사에서 제외한다(자기 자신과 비교해
     * 항상 중복으로 걸리는 것을 방지). 기숙사 정보가 아직 없는 사용자가 처음 동을 등록하는 경우도
     * 이 API로 처리하며, 이때는 인증 사진 없이 NOT_SUBMITTED 상태로 row를 새로 만든다.
     */
    @Transactional
    public ProfileUpdateResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        String newNickname = request.nickname();
        if (!newNickname.equals(user.getNickname()) && userRepository.existsByNickname(newNickname)) {
            throw new UserException(UserErrorCode.NICKNAME_DUPLICATE);
        }

        // profileImageUrl 필드 자체를 생략(null)했으면 기존 사진을 그대로 두고, 명시적으로 빈
        // 문자열을 보냈을 때만 삭제(null)로 반영한다 - 그래야 닉네임/기숙사 동만 바꾸는 요청이
        // 매번 사진을 지워버리는 사고를 피할 수 있다(DTO의 @Pattern("^$")이 null 또는 빈 문자열만
        // 통과시키므로 여기 도달하는 시점엔 그 둘 중 하나뿐이다).
        String newProfileImageUrl = request.profileImageUrl();
        String profileImageUrlToSave = newProfileImageUrl == null
                ? user.getProfileImageUrl()
                : (newProfileImageUrl.isBlank() ? null : newProfileImageUrl);
        user.updateProfile(newNickname, profileImageUrlToSave);
        userRepository.save(user);

        Dormitory dormitory = dormitoryRepository.findByUserId(userId).orElse(null);
        if (dormitory == null) {
            dormitory = new Dormitory(userId, request.dormitory(), null);
            dormitoryRepository.save(dormitory);
        } else {
            dormitory.changeDormitory(request.dormitory());
        }

        return new ProfileUpdateResponse(user.getNickname(), dormitory.getDormitory(), user.getProfileImageUrl());
    }

    /**
     * 마이페이지 > 프로필 사진 업로드 1단계(발급). 기숙사 인증과 같은 presign/confirm 흐름이지만
     * 공개 버킷을 쓴다. 프로필은 승인 절차가 없어(기숙사와 달리 "이미 진행 중" 같은 충돌 상태가
     * 없음) DB 조회 없이 바로 key + Presigned PUT URL을 발급한다.
     */
    @Transactional(readOnly = true)
    public ProfileImagePresignResponse presignProfileImageUpload(Long userId, ProfileImagePresignRequest request) {
        PresignedUploadResponse presigned =
                imageStorageService.issueUploadUrl(ImageCategory.PROFILE, userId, request.contentType());

        return new ProfileImagePresignResponse(presigned.key(), presigned.uploadUrl());
    }

    /**
     * 마이페이지 > 프로필 사진 업로드 2단계(확정). 클라이언트가 보낸 key를 그대로 신뢰하지 않고
     * (1) 호출자 본인 몫의 key인지, (2) 실제로 S3(공개 버킷)에 업로드가 됐는지, (3) 용량/형식이
     * 기준 안에 있는지를 검증한 뒤에만 User.profileImageUrl(실제로는 key 저장)에 반영한다.
     * 응답의 profileImageUrl은 공개 버킷 base URL과 key를 조립한 완성된 URL이다(만료 없음).
     */
    @Transactional
    public ProfileImageUploadResponse confirmProfileImageUpload(Long userId, ProfileImageConfirmRequest request) {
        String key = request.key();

        if (!belongsToUser(ImageCategory.PROFILE, userId, key)) {
            throw new UserException(UserErrorCode.INVALID_PROFILE_IMAGE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 용량/형식 기준을 벗어나면 confirmUpload가 ImageException을 던지면서 객체도 함께 지운다.
        imageStorageService.confirmUpload(key);

        String previousKey = user.getProfileImageUrl();
        user.updateProfileImageKey(key);
        userRepository.save(user);

        // 새 key 반영이 끝난 뒤에만 예전 사진을 지운다 - 그래야 중간에 실패해도 예전 사진은
        // 그대로 남는다(고아 객체가 남는 대신 데이터를 잃는 사고를 우선 피한다). 같은 key로
        // confirm이 재호출된 경우(previousKey == key)는 방금 반영한 새 사진을 지우면 안 되니 제외한다.
        if (previousKey != null && !previousKey.equals(key)) {
            imageStorageService.delete(previousKey);
        }

        return new ProfileImageUploadResponse(imageStorageService.resolveViewUrl(key));
    }

    /**
     * 마이페이지 > 기숙사 인증하기 1단계(발급). 브라우저가 S3에 직접 올릴 수 있도록 key와
     * Presigned PUT URL을 발급한다. DB는 여기서 건드리지 않는다(확정 단계에서만 반영).
     * 이미 PENDING(심사중)이거나 APPROVED(승인)면 업로드를 시작할 필요가 없으니 여기서 막는다.
     */
    @Transactional(readOnly = true)
    public DormVerificationPresignResponse presignDormVerificationUpload(
            Long userId, DormVerificationPresignRequest request
    ) {
        Dormitory dormitory = dormitoryRepository.findByUserId(userId).orElse(null);
        if (dormitory != null && dormitory.isVerificationInProgress()) {
            throw new UserException(UserErrorCode.DORM_VERIFICATION_ALREADY_IN_PROGRESS);
        }

        PresignedUploadResponse presigned = imageStorageService.issueUploadUrl(
                ImageCategory.DORMITORY_VERIFICATION, userId, request.contentType());

        return new DormVerificationPresignResponse(presigned.key(), presigned.uploadUrl());
    }

    /**
     * 마이페이지 > 기숙사 인증하기 3단계(확정). 브라우저가 S3에 직접 올린 뒤 호출한다.
     * 클라이언트가 보낸 key를 그대로 신뢰하지 않고, (1) 호출자 본인 몫의 key인지, (2) 실제로
     * S3에 업로드가 됐는지, (3) 용량/형식이 기준 안에 있는지를 순서대로 검증한 뒤에만 DB에
     * 반영해 PENDING(심사 대기) 상태로 바꾼다. 검증에 실패하면 업로드된 객체를 지워서 버킷에
     * 고아 객체가 남지 않게 한다.
     */
    @Transactional
    public DormVerificationUploadResponse confirmDormVerificationUpload(
            Long userId, DormVerificationConfirmRequest request
    ) {
        String key = request.key();

        if (!belongsToUser(ImageCategory.DORMITORY_VERIFICATION, userId, key)) {
            throw new UserException(UserErrorCode.INVALID_DORM_VERIFICATION_IMAGE);
        }

        Dormitory dormitory = dormitoryRepository.findByUserId(userId).orElse(null);
        if (dormitory != null && dormitory.isVerificationInProgress()) {
            imageStorageService.delete(key);
            throw new UserException(UserErrorCode.DORM_VERIFICATION_ALREADY_IN_PROGRESS);
        }

        // 용량/형식 기준을 벗어나면 confirmUpload가 ImageException을 던지면서 객체도 함께 지운다.
        imageStorageService.confirmUpload(key);

        if (dormitory == null) {
            dormitory = new Dormitory(userId, null, key);
            dormitoryRepository.save(dormitory);
        } else {
            String previousKey = dormitory.getDormVerificationImageKey();
            dormitory.resubmit(dormitory.getDormitory(), key);
            // 재제출로 새 key가 반영된 뒤에만 예전(반려됐던) 인증 사진을 지운다 - 반려 사유로
            // 즉시 삭제하지 않던 사진도, 재제출로 더 이상 참조되지 않게 된 시점엔 정리해야 한다.
            if (previousKey != null && !previousKey.equals(key)) {
                imageStorageService.delete(previousKey);
            }
        }

        return new DormVerificationUploadResponse(
                dormitory.getDormStatus().name(),
                dormitory.getDormVerifiedAt(),
                dormitory.getDormVerifiedUntil(),
                imageStorageService.resolveViewUrl(key)
        );
    }

    /**
     * 마이페이지 > 알림 설정. 전체 알림 on/off 통합 토글 하나만 갱신한다(MVP 범위).
     * 카테고리 구분, 다른 도메인 조회/연동 없이 User.pushEnabled만 바꾼다.
     */
    @Transactional
    public PushSettingResponse updatePushSetting(Long userId, PushSettingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        user.updatePushEnabled(request.pushEnabled());
        userRepository.save(user);

        return new PushSettingResponse(user.isPushEnabled());
    }

    /**
     * 마이페이지 > 알림 구독 등록. 브라우저 PushManager.subscribe()로 발급받은 endpoint/키를
     * 저장한다(Web Push 방식, FCM 아님 - User 엔티티 컬럼이 이미 endpoint/p256dh/auth 구조).
     * 기존 구독이 있어도 그냥 덮어쓴다(기기 교체/브라우저 재설치 시 재구독하는 흔한 케이스라
     * 별도 중복 에러 없이 최신 값으로 갱신). pushEnabled는 건드리지 않는다(updatePushSetting의
     * 별도 관심사).
     */
    @Transactional
    public void registerPushSubscription(Long userId, PushSubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        user.updatePushSubscription(request.endpoint(), request.p256dhKey(), request.authKey());
        userRepository.save(user);
    }

    /**
     * 마이페이지 > 비밀번호 수정. 현재 비밀번호로 본인 확인 후 새 비밀번호로 바꾸고,
     * 기존 refresh token을 무효화한다(clearRefreshToken - 로그아웃/탈퇴와 동일한 관례).
     * access token 자체는 상태 없는 JWT라 만료 전까지는 계속 쓸 수 있고, 재로그인은
     * refresh token으로 재발급받아야 할 때만 필요해진다.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new UserException(UserErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new UserException(UserErrorCode.NEW_PASSWORD_SAME_AS_CURRENT);
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        user.clearRefreshToken();
        userRepository.save(user);
    }

    /**
     * 계정탈퇴. access token으로 신원은 이미 확인됐으니, 비밀번호 재검증 후 soft delete(status
     * = DELETED) 처리하고 refresh token도 무효화한다(로그아웃과 동일하게 clearRefreshToken()).
     * suspendedUntil/noShowApprovedCount/mannerTemperature는 softDelete()가 건드리지 않으므로
     * 그대로 유지된다(정지 우회 방지 + 재가입 시 이력 복원).
     * <p>
     * 방장/참여자 구분 없이 본인이 속한 팟 중 진행 중(RECRUITING/CLOSED/ORDERED)인 팟이 있으면
     * ACTIVE_POT_EXISTS로, COMPLETED인데 정산이 아직 안 끝난(SettlementStatus가 COMPLETED/CANCELED가
     * 아닌) 팟이 있으면 UNSETTLED_POT_EXISTS로 탈퇴를 막는다. 방장 탈퇴로 정산 트리거가 사라지는 문제,
     * 참여자가 노쇼 신고 전에 탈퇴로 회피하는 문제를 둘 다 막기 위함이다.
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

        List<Long> joinedPartyIds = joinedPartyIds(userId);

        if (hasOngoingDeliveryParty(userId, joinedPartyIds)) {
            throw new UserException(UserErrorCode.ACTIVE_POT_EXISTS);
        }
        if (hasUnsettledDeliveryParty(userId, joinedPartyIds)) {
            throw new UserException(UserErrorCode.UNSETTLED_POT_EXISTS);
        }

        user.softDelete();
        user.clearRefreshToken();
        userRepository.save(user);
    }

    // 참여를 취소하지 않은(JOINED) 팟 id 목록 - 참여자(participant) 기준 체크에 재사용
    private List<Long> joinedPartyIds(Long userId) {
        return partyParticipantRepository
                .findAllByUserIdAndStatus(userId, PartyParticipantStatus.JOINED)
                .stream()
                .map(PartyParticipant::getPartyId)
                .collect(Collectors.toList());
    }

    // RECRUITING/CLOSED/ORDERED로 진행 중인 팟이 있는지 (방장 + 참여자 기준)
    private boolean hasOngoingDeliveryParty(Long userId, List<Long> joinedPartyIds) {
        if (deliveryPartyRepository.existsByCreatorIdAndStatusIn(userId, ONGOING_PARTY_STATUSES)) {
            return true;
        }
        if (joinedPartyIds.isEmpty()) {
            return false;
        }
        return deliveryPartyRepository.existsByIdInAndStatusIn(joinedPartyIds, ONGOING_PARTY_STATUSES);
    }

    // COMPLETED인데 정산이 아직 안 끝난 팟이 있는지 (방장 + 참여자 기준)
    private boolean hasUnsettledDeliveryParty(Long userId, List<Long> joinedPartyIds) {
        if (deliveryPartyRepository.existsByCreatorIdAndStatusAndSettlementStatusNotIn(
                userId, PartyStatus.COMPLETED, SETTLEMENT_TERMINAL_STATUSES)) {
            return true;
        }
        if (joinedPartyIds.isEmpty()) {
            return false;
        }
        return deliveryPartyRepository.existsByIdInAndStatusAndSettlementStatusNotIn(
                joinedPartyIds, PartyStatus.COMPLETED, SETTLEMENT_TERMINAL_STATUSES);
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

    /**
     * key가 category/userId/ 아래에 있는(=본인 몫의) 단일 세그먼트 key인지 검증한다. 공용
     * ImageStorageService.confirmUpload(key)는 소유권을 검증하지 않으므로(용량/형식만 검증),
     * presign 단계에서 클라이언트가 보낸 key를 confirm 단계에서 그대로 신뢰하지 않기 위해
     * 여기서 별도로 확인한다 - key에 적힌 id 자체를 권한 근거로 삼지 않기 위함이다.
     */
    private boolean belongsToUser(ImageCategory category, Long userId, String key) {
        if (key == null) {
            return false;
        }
        String expectedPrefix = "%s/%d/".formatted(category.getPrefix(), userId);
        if (!key.startsWith(expectedPrefix)) {
            return false;
        }
        String remainder = key.substring(expectedPrefix.length());
        return !remainder.isEmpty() && !remainder.contains("/") && !remainder.contains("..");
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
