package com.leets.tdd.user.service;

import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.global.jwt.RefreshTokenHasher;
import com.leets.tdd.global.storage.ImageCategory;
import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
import com.leets.tdd.global.storage.exception.ImageErrorCode;
import com.leets.tdd.global.storage.exception.ImageException;
import com.leets.tdd.auth.service.EmailVerificationService;
import com.leets.tdd.party.domain.PartyParticipant;
import com.leets.tdd.party.domain.PartyParticipantRole;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import com.leets.tdd.settlement.domain.SettlementStatus;
import com.leets.tdd.user.domain.DormStatus;
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
import com.leets.tdd.user.dto.WithdrawalRequest;
import com.leets.tdd.user.dto.ChangePasswordRequest;
import com.leets.tdd.user.dto.ProfileUpdateRequest;
import com.leets.tdd.user.dto.ProfileUpdateResponse;
import com.leets.tdd.user.dto.PushSettingRequest;
import com.leets.tdd.user.dto.PushSettingResponse;
import com.leets.tdd.user.dto.PushSubscriptionRequest;
import com.leets.tdd.user.exception.UserErrorCode;
import com.leets.tdd.user.exception.UserException;
import com.leets.tdd.user.repository.DormitoryRepository;
import com.leets.tdd.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DormitoryRepository dormitoryRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @Mock
    private DeliveryPartyRepository deliveryPartyRepository;

    @Mock
    private PartyParticipantRepository partyParticipantRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private UserService userService;

    private User newUser() {
        return new User("abcd@gachon.ac.kr", "가나디", "encoded-pw",
                "refresh-hash", LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("유저가 없으면 예외가 발생한다")
    void getMyPage_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyPage(1L))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("기숙사 인증 정보가 없으면(미인증) 관련 필드가 전부 null로 내려간다")
    void getMyPage_noDormitory() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());

        MyPageResponse response = userService.getMyPage(1L);

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
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "s3-key");
        dormitory.approve(LocalDateTime.now().plusMonths(4));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        MyPageResponse response = userService.getMyPage(1L);

        assertThat(response.dormitory()).isEqualTo("1기숙사");
        assertThat(response.dormStatus()).isEqualTo(DormStatus.APPROVED.name());
        assertThat(response.dormVerifiedUntil()).isNotNull();
    }

    @Test
    @DisplayName("기숙사 인증이 거절됐으면 거절 사유가 응답에 포함된다")
    void getMyPage_dormitoryRejected() {
        User user = newUser();
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "s3-key");
        dormitory.reject("사진이 흐릿합니다");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        MyPageResponse response = userService.getMyPage(1L);

        assertThat(response.dormStatus()).isEqualTo(DormStatus.REJECTED.name());
        assertThat(response.rejectReason()).isEqualTo("사진이 흐릿합니다");
    }

    @Test
    @DisplayName("동만 입력하고 사진을 제출하지 않았으면 NOT_SUBMITTED로 내려간다")
    void getMyPage_dormitoryNotSubmitted() {
        User user = newUser();
        Dormitory dormitory = new Dormitory(1L, "1기숙사", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        MyPageResponse response = userService.getMyPage(1L);

        assertThat(response.dormitory()).isEqualTo("1기숙사");
        assertThat(response.dormStatus()).isEqualTo(DormStatus.NOT_SUBMITTED.name());
        assertThat(response.dormVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("프로필 사진이 있으면 key가 아니라 resolveViewUrl로 조립한 URL이 내려간다")
    void getMyPage_withProfileImage_returnsResolvedUrl() {
        User user = newUser();
        user.updateProfileImageKey("profiles/1/uuid.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(imageStorageService.resolveViewUrl("profiles/1/uuid.jpg"))
                .thenReturn("https://assets-public.example.com/profiles/1/uuid.jpg");

        MyPageResponse response = userService.getMyPage(1L);

        assertThat(response.profileImageUrl()).isEqualTo("https://assets-public.example.com/profiles/1/uuid.jpg");
    }

    @Test
    @DisplayName("정지 기간이 지났으면 조회 시 자동으로 ACTIVE 상태가 된다")
    void getMyPage_liftsExpiredSuspension() {
        User user = newUser();
        user.suspend(LocalDateTime.now().minusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());

        MyPageResponse response = userService.getMyPage(1L);

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.suspendedUntil()).isNull();
    }

    private ProfileRegistrationRequest newRequest(String nickname, String dormitory) {
        return new ProfileRegistrationRequest("abcd@gachon.ac.kr", "a12345", nickname, dormitory);
    }

    @Test
    @DisplayName("15분 이내 이메일 인증 기록이 없으면 예외가 발생한다")
    void completeSignup_notVerified() {
        when(emailVerificationService.consumeSignupVerification("abcd@gachon.ac.kr")).thenReturn(false);

        assertThatThrownBy(() -> userService.completeSignup(newRequest("가나디", "1기숙사")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.INVALID_VERIFICATION.getMessage());
    }

    @Test
    @DisplayName("입력한 닉네임이 중복이면 예외가 발생한다")
    void completeSignup_nicknameDuplicate() {
        when(emailVerificationService.consumeSignupVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.existsByNickname("가나디")).thenReturn(true);

        assertThatThrownBy(() -> userService.completeSignup(newRequest("가나디", "1기숙사")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.NICKNAME_DUPLICATE.getMessage());
    }

    @Test
    @DisplayName("신규 가입이면 User를 생성하고 토큰을 발급한다")
    void completeSignup_newUserSuccess() {
        when(emailVerificationService.consumeSignupVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.existsByNickname("가나디")).thenReturn(false);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProvider.getRefreshTokenValidity()).thenReturn(Duration.ofDays(30));
        when(refreshTokenHasher.hash(anyString())).thenReturn("hashed-refresh-token");
        when(dormitoryRepository.findByUserId(any())).thenReturn(Optional.empty());

        ProfileRegistrationResponse response = userService.completeSignup(newRequest("가나디", "1기숙사"));

        assertThat(response.nickname()).isEqualTo("가나디");
        assertThat(response.dormitory()).isEqualTo("1기숙사");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");

        verify(userRepository, times(2)).save(any(User.class));
        verify(dormitoryRepository).save(any(Dormitory.class));
        verify(emailVerificationService).consumeSignupVerification("abcd@gachon.ac.kr");
    }

    @Test
    @DisplayName("닉네임을 생략하면 자동 배정한다")
    void completeSignup_nicknameAutoAssign() {
        when(emailVerificationService.consumeSignupVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(nicknameGenerator.generate()).thenReturn("행복한가나디1234");
        when(userRepository.existsByNickname("행복한가나디1234")).thenReturn(false);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProvider.getRefreshTokenValidity()).thenReturn(Duration.ofDays(30));
        when(refreshTokenHasher.hash(anyString())).thenReturn("hashed-refresh-token");

        ProfileRegistrationResponse response = userService.completeSignup(newRequest(null, null));

        assertThat(response.nickname()).isEqualTo("행복한가나디1234");
        verify(dormitoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("ACTIVE/SUSPENDED 계정이 있으면 이미 가입된 이메일로 처리한다")
    void completeSignup_alreadyRegisteredActiveUser() {
        User existing = newUser();
        when(emailVerificationService.consumeSignupVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.existsByNickname("가나디")).thenReturn(false);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.completeSignup(newRequest("가나디", null)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.ALREADY_REGISTERED_EMAIL.getMessage());
    }

    @Test
    @DisplayName("BANNED 계정이면 가입할 수 없다")
    void completeSignup_bannedUser() {
        User banned = newUser();
        banned.ban();
        when(emailVerificationService.consumeSignupVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.existsByNickname("가나디")).thenReturn(false);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> userService.completeSignup(newRequest("가나디", null)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.REGISTRATION_BLOCKED.getMessage());
    }

    @Test
    @DisplayName("탈퇴했지만 정지기간이 아직 안 지났으면 가입할 수 없다(정지 우회 방지)")
    void completeSignup_deletedWithinSuspension() {
        User deleted = newUser();
        deleted.suspend(LocalDateTime.now().plusDays(3));
        deleted.softDelete();
        when(emailVerificationService.consumeSignupVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.existsByNickname("가나디")).thenReturn(false);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> userService.completeSignup(newRequest("가나디", null)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.REGISTRATION_BLOCKED.getMessage());
    }

    @Test
    @DisplayName("탈퇴했고 정지기간도 지났으면 재가입(reactivate) 처리한다")
    void completeSignup_reactivateDeletedUser() {
        User deleted = newUser();
        deleted.softDelete();
        when(emailVerificationService.consumeSignupVerification("abcd@gachon.ac.kr")).thenReturn(true);
        when(userRepository.existsByNickname("가나디")).thenReturn(false);
        when(userRepository.findByEmail("abcd@gachon.ac.kr")).thenReturn(Optional.of(deleted));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(jwtProvider.createAccessToken(any())).thenReturn("access-token");
        when(jwtProvider.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProvider.getRefreshTokenValidity()).thenReturn(Duration.ofDays(30));
        when(refreshTokenHasher.hash(anyString())).thenReturn("hashed-refresh-token");

        ProfileRegistrationResponse response = userService.completeSignup(newRequest("가나디", null));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(deleted.getStatus().name()).isEqualTo("ACTIVE");
        verify(userRepository, times(1)).save(any(User.class));
    }

    private ProfileUpdateRequest updateRequest(String nickname, String dormitory, String profileImageUrl) {
        return new ProfileUpdateRequest(nickname, dormitory, profileImageUrl);
    }

    @Test
    @DisplayName("유저가 없으면 프로필 수정 시 예외가 발생한다")
    void updateProfile_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(1L, updateRequest("가나디", "1기숙사", null)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("변경하려는 닉네임이 다른 사람이 쓰는 중이면 예외가 발생한다")
    void updateProfile_nicknameDuplicate() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("다른닉네임")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L, updateRequest("다른닉네임", "1기숙사", null)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.NICKNAME_DUPLICATE.getMessage());
    }

    @Test
    @DisplayName("닉네임을 기존과 동일하게 보내면 중복 검사를 하지 않는다")
    void updateProfile_sameNicknameSkipsDuplicateCheck() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());

        userService.updateProfile(1L, updateRequest("가나디", "1기숙사", null));

        verify(userRepository, never()).existsByNickname(anyString());
    }

    @Test
    @DisplayName("기숙사 정보가 없던 사용자면 새로 생성한다")
    void updateProfile_createsDormitoryWhenAbsent() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("새닉네임")).thenReturn(false);
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // profileImageUrl은 이 API로 설정할 수 없다(업로드 API 전용) - null만 넘긴다.
        ProfileUpdateResponse response = userService.updateProfile(
                1L, updateRequest("새닉네임", "2기숙사", null));

        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.dormitory()).isEqualTo("2기숙사");
        assertThat(response.profileImageUrl()).isNull();
        verify(dormitoryRepository).save(any(Dormitory.class));
    }

    @Test
    @DisplayName("기존 기숙사 정보가 있으면 인증 상태는 유지한 채 동만 바꾼다")
    void updateProfile_updatesExistingDormitoryWithoutTouchingVerification() {
        User user = newUser();
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "s3-key");
        dormitory.approve(LocalDateTime.now().plusMonths(4));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("가나디2")).thenReturn(false);
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        ProfileUpdateResponse response = userService.updateProfile(
                1L, updateRequest("가나디2", "3기숙사", null));

        assertThat(response.dormitory()).isEqualTo("3기숙사");
        assertThat(dormitory.getDormStatus()).isEqualTo(DormStatus.APPROVED);
        assertThat(dormitory.getDormVerifiedUntil()).isNotNull();
        verify(dormitoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("profileImageUrl을 생략(null)하면 기존 프로필 사진을 그대로 유지한다")
    void updateProfile_omitsProfileImageUrl_preservesExistingPhoto() {
        User user = newUser();
        user.updateProfileImageKey("profiles/1/existing.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("새닉네임")).thenReturn(false);
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(imageStorageService.resolveViewUrl("profiles/1/existing.jpg"))
                .thenReturn("https://assets-public.example.com/profiles/1/existing.jpg");

        ProfileUpdateResponse response = userService.updateProfile(
                1L, updateRequest("새닉네임", "2기숙사", null));

        assertThat(response.profileImageUrl()).isEqualTo("https://assets-public.example.com/profiles/1/existing.jpg");
        assertThat(user.getProfileImageUrl()).isEqualTo("profiles/1/existing.jpg");
    }

    @Test
    @DisplayName("profileImageUrl에 빈 문자열을 명시적으로 보내면 프로필 사진을 삭제한다")
    void updateProfile_explicitEmptyProfileImageUrl_clearsPhoto() {
        User user = newUser();
        user.updateProfileImageKey("profiles/1/existing.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("새닉네임")).thenReturn(false);
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());

        ProfileUpdateResponse response = userService.updateProfile(
                1L, updateRequest("새닉네임", "2기숙사", ""));

        assertThat(response.profileImageUrl()).isNull();
        assertThat(user.getProfileImageUrl()).isNull();
    }

    // ===== presignProfileImageUpload =====

    private static final String PROFILE_KEY = "profiles/1/uuid.jpg";
    private static final String PROFILE_KEY_OTHER_USER = "profiles/2/uuid.jpg";

    @Test
    @DisplayName("발급 요청을 보내면 key와 업로드 URL을 받는다(DB 조회 없음)")
    void presignProfileImageUpload_success_returnsKeyAndUrl() {
        when(imageStorageService.issueUploadUrl(ImageCategory.PROFILE, 1L, "image/jpeg"))
                .thenReturn(new PresignedUploadResponse(PROFILE_KEY, "https://presigned.example.com/put",
                        "image/jpeg", 300L));

        ProfileImagePresignResponse response =
                userService.presignProfileImageUpload(1L, new ProfileImagePresignRequest("image/jpeg"));

        assertThat(response.key()).isEqualTo(PROFILE_KEY);
        assertThat(response.uploadUrl()).isEqualTo("https://presigned.example.com/put");
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("허용되지 않는 형식이면 발급이 거부된다(공용 이미지 모듈의 예외가 그대로 전파된다)")
    void presignProfileImageUpload_invalidContentType_throwsImageException() {
        when(imageStorageService.issueUploadUrl(ImageCategory.PROFILE, 1L, "image/gif"))
                .thenThrow(new ImageException(ImageErrorCode.UNSUPPORTED_CONTENT_TYPE));

        assertThatThrownBy(() -> userService.presignProfileImageUpload(
                1L, new ProfileImagePresignRequest("image/gif")))
                .isInstanceOf(ImageException.class)
                .hasMessage(ImageErrorCode.UNSUPPORTED_CONTENT_TYPE.getMessage());
    }

    // ===== confirmProfileImageUpload =====

    @Test
    @DisplayName("검증을 통과하면 프로필 사진이 갱신되고 완성된 공개 URL을 응답한다")
    void confirmProfileImageUpload_success_updatesProfileImage() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageStorageService.resolveViewUrl(PROFILE_KEY))
                .thenReturn("https://public.example.com/" + PROFILE_KEY);

        ProfileImageUploadResponse response =
                userService.confirmProfileImageUpload(1L, new ProfileImageConfirmRequest(PROFILE_KEY));

        assertThat(response.profileImageUrl()).isEqualTo("https://public.example.com/" + PROFILE_KEY);
        assertThat(user.getProfileImageUrl()).isEqualTo(PROFILE_KEY);
        verify(imageStorageService).confirmUpload(PROFILE_KEY);
        verify(userRepository).save(user);
        verify(imageStorageService, never()).delete(any());
    }

    @Test
    @DisplayName("기존 프로필 사진이 있었으면 새 사진 반영 후 예전 사진을 S3에서 지운다")
    void confirmProfileImageUpload_replacesExistingPhoto_deletesOldKey() {
        User user = newUser();
        user.updateProfileImageKey("profiles/1/old.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(imageStorageService.resolveViewUrl(PROFILE_KEY))
                .thenReturn("https://public.example.com/" + PROFILE_KEY);

        userService.confirmProfileImageUpload(1L, new ProfileImageConfirmRequest(PROFILE_KEY));

        assertThat(user.getProfileImageUrl()).isEqualTo(PROFILE_KEY);
        verify(imageStorageService).delete("profiles/1/old.jpg");
    }

    @Test
    @DisplayName("본인 몫이 아닌 key면 확정이 거부되고 S3/DB 조회조차 하지 않는다")
    void confirmProfileImageUpload_keyNotOwned_throwsUserException() {
        assertThatThrownBy(() -> userService.confirmProfileImageUpload(
                1L, new ProfileImageConfirmRequest(PROFILE_KEY_OTHER_USER)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.INVALID_PROFILE_IMAGE.getMessage());

        verify(userRepository, never()).findById(any());
        verify(imageStorageService, never()).confirmUpload(any());
    }

    @Test
    @DisplayName("유저가 없으면 확정 시 예외가 발생한다")
    void confirmProfileImageUpload_userNotFound_throwsUserException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.confirmProfileImageUpload(
                1L, new ProfileImageConfirmRequest(PROFILE_KEY)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

        verify(imageStorageService, never()).confirmUpload(any());
    }

    @Test
    @DisplayName("실제로 업로드되지 않은 key면 확정이 거부된다(공용 이미지 모듈의 예외가 그대로 전파된다)")
    void confirmProfileImageUpload_objectNotUploaded_throwsImageException() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new ImageException(ImageErrorCode.IMAGE_NOT_UPLOADED))
                .when(imageStorageService).confirmUpload(PROFILE_KEY);

        assertThatThrownBy(() -> userService.confirmProfileImageUpload(
                1L, new ProfileImageConfirmRequest(PROFILE_KEY)))
                .isInstanceOf(ImageException.class)
                .hasMessage(ImageErrorCode.IMAGE_NOT_UPLOADED.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("업로드된 객체가 용량/형식 기준을 벗어나면 거부한다(삭제는 공용 모듈이 처리한다)")
    void confirmProfileImageUpload_invalidUpload_throwsImageException() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new ImageException(ImageErrorCode.IMAGE_TOO_LARGE))
                .when(imageStorageService).confirmUpload(PROFILE_KEY);

        assertThatThrownBy(() -> userService.confirmProfileImageUpload(
                1L, new ProfileImageConfirmRequest(PROFILE_KEY)))
                .isInstanceOf(ImageException.class)
                .hasMessage(ImageErrorCode.IMAGE_TOO_LARGE.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("유저가 없으면 알림 설정 변경 시 예외가 발생한다")
    void updatePushSetting_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePushSetting(1L, new PushSettingRequest(false)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("알림 설정을 끄면 pushEnabled가 false로 바뀐다")
    void updatePushSetting_disables() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PushSettingResponse response = userService.updatePushSetting(1L, new PushSettingRequest(false));

        assertThat(response.pushEnabled()).isFalse();
        assertThat(user.isPushEnabled()).isFalse();
    }

    @Test
    @DisplayName("알림 설정을 켜면 pushEnabled가 true로 바뀐다")
    void updatePushSetting_enables() {
        User user = newUser();
        user.updatePushEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PushSettingResponse response = userService.updatePushSetting(1L, new PushSettingRequest(true));

        assertThat(response.pushEnabled()).isTrue();
    }

    @Test
    @DisplayName("유저가 없으면 알림 구독 등록 시 예외가 발생한다")
    void registerPushSubscription_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.registerPushSubscription(
                1L, new PushSubscriptionRequest("https://fcm.googleapis.com/endpoint", "p256dh-key", "auth-key")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("구독 정보를 등록하면 endpoint/키가 저장된다")
    void registerPushSubscription_success() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.registerPushSubscription(
                1L, new PushSubscriptionRequest("https://fcm.googleapis.com/endpoint", "p256dh-key", "auth-key"));

        assertThat(user.getPushEndpoint()).isEqualTo("https://fcm.googleapis.com/endpoint");
        assertThat(user.getPushP256dhKey()).isEqualTo("p256dh-key");
        assertThat(user.getPushAuthKey()).isEqualTo("auth-key");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("이미 구독 정보가 있어도 새 값으로 덮어쓴다(재구독)")
    void registerPushSubscription_overwritesExisting() {
        User user = newUser();
        user.updatePushSubscription("old-endpoint", "old-p256dh", "old-auth");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.registerPushSubscription(
                1L, new PushSubscriptionRequest("new-endpoint", "new-p256dh", "new-auth"));

        assertThat(user.getPushEndpoint()).isEqualTo("new-endpoint");
        assertThat(user.getPushP256dhKey()).isEqualTo("new-p256dh");
        assertThat(user.getPushAuthKey()).isEqualTo("new-auth");
    }

    @Test
    @DisplayName("구독을 등록해도 알림 on/off 설정(pushEnabled)은 건드리지 않는다")
    void registerPushSubscription_doesNotChangePushEnabled() {
        User user = newUser();
        user.updatePushEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.registerPushSubscription(
                1L, new PushSubscriptionRequest("https://fcm.googleapis.com/endpoint", "p256dh-key", "auth-key"));

        assertThat(user.isPushEnabled()).isFalse();
    }

    @Test
    @DisplayName("유저가 없으면 비밀번호 수정 시 예외가 발생한다")
    void changePassword_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest("현재비번", "새비번1234")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하지 않으면 예외가 발생한다")
    void changePassword_currentPasswordMismatch() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("틀린비번", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest("틀린비번", "새비번1234")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.CURRENT_PASSWORD_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("새 비밀번호가 기존과 같으면 예외가 발생한다")
    void changePassword_sameAsCurrent() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("현재비번", user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest("현재비번", "현재비번")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.NEW_PASSWORD_SAME_AS_CURRENT.getMessage());
    }

    @Test
    @DisplayName("검증을 통과하면 비밀번호를 바꾸고 refresh token을 무효화한다")
    void changePassword_success() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("현재비번", user.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("새비번1234", user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("새비번1234")).thenReturn("encoded-new-pw");

        userService.changePassword(1L, new ChangePasswordRequest("현재비번", "새비번1234"));

        assertThat(user.getPassword()).isEqualTo("encoded-new-pw");
        assertThat(user.getRefreshTokenHash()).isEmpty();
        verify(userRepository).save(user);
    }

    // ===== presignDormVerificationUpload =====

    private static final String KEY = "dormitory-verifications/1/uuid.jpg";
    private static final String KEY_OTHER_USER = "dormitory-verifications/2/uuid.jpg";

    @Test
    @DisplayName("기숙사 정보가 없어도 발급은 가능하다(DB는 안 건드림)")
    void presignDormVerificationUpload_noExistingDormitory_returnsKeyAndUrl() {
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(imageStorageService.issueUploadUrl(ImageCategory.DORMITORY_VERIFICATION, 1L, "image/jpeg"))
                .thenReturn(new PresignedUploadResponse(KEY, "https://presigned.example.com/put",
                        "image/jpeg", 300L));

        DormVerificationPresignResponse response =
                userService.presignDormVerificationUpload(1L, new DormVerificationPresignRequest("image/jpeg"));

        assertThat(response.key()).isEqualTo(KEY);
        assertThat(response.uploadUrl()).isEqualTo("https://presigned.example.com/put");
        verify(dormitoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("거절됐던 기숙사 인증도 다시 발급받을 수 있다")
    void presignDormVerificationUpload_rejectedDormitory_returnsKeyAndUrl() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "old-key");
        dormitory.reject("사진이 흐릿합니다");
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));
        when(imageStorageService.issueUploadUrl(ImageCategory.DORMITORY_VERIFICATION, 1L, "image/png"))
                .thenReturn(new PresignedUploadResponse(KEY, "https://presigned.example.com/put",
                        "image/png", 300L));

        DormVerificationPresignResponse response =
                userService.presignDormVerificationUpload(1L, new DormVerificationPresignRequest("image/png"));

        assertThat(response.key()).isEqualTo(KEY);
    }

    @Test
    @DisplayName("이미 심사 중(PENDING)이면 발급 자체가 거부된다")
    void presignDormVerificationUpload_alreadyPending_throwsConflict() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "existing-key");
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        assertThatThrownBy(() -> userService.presignDormVerificationUpload(
                1L, new DormVerificationPresignRequest("image/jpeg")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.DORM_VERIFICATION_ALREADY_IN_PROGRESS.getMessage());

        verify(imageStorageService, never()).issueUploadUrl(any(), anyLong(), any());
    }

    @Test
    @DisplayName("이미 승인(APPROVED)된 상태면 발급 자체가 거부된다")
    void presignDormVerificationUpload_alreadyApproved_throwsConflict() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "existing-key");
        dormitory.approve(LocalDateTime.now().plusMonths(4));
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        assertThatThrownBy(() -> userService.presignDormVerificationUpload(
                1L, new DormVerificationPresignRequest("image/jpeg")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.DORM_VERIFICATION_ALREADY_IN_PROGRESS.getMessage());
    }

    @Test
    @DisplayName("허용되지 않는 형식이면 발급이 거부된다(공용 이미지 모듈의 예외가 그대로 전파된다)")
    void presignDormVerificationUpload_invalidContentType_throwsImageException() {
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(imageStorageService.issueUploadUrl(ImageCategory.DORMITORY_VERIFICATION, 1L, "image/gif"))
                .thenThrow(new ImageException(ImageErrorCode.UNSUPPORTED_CONTENT_TYPE));

        assertThatThrownBy(() -> userService.presignDormVerificationUpload(
                1L, new DormVerificationPresignRequest("image/gif")))
                .isInstanceOf(ImageException.class)
                .hasMessage(ImageErrorCode.UNSUPPORTED_CONTENT_TYPE.getMessage());
    }

    // ===== confirmDormVerificationUpload =====

    @Test
    @DisplayName("검증을 통과하면 기숙사 정보가 없던 사용자도 새로 생성되고 PENDING이 된다")
    void confirmDormVerificationUpload_noExistingDormitory_createsNewPending() {
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(imageStorageService.resolveViewUrl(KEY)).thenReturn("https://presigned.example.com/get");

        DormVerificationUploadResponse response =
                userService.confirmDormVerificationUpload(1L, new DormVerificationConfirmRequest(KEY));

        assertThat(response.dormStatus()).isEqualTo(DormStatus.PENDING.name());
        assertThat(response.dormVerifiedAt()).isNull();
        assertThat(response.dormVerifiedImageUrl()).isEqualTo("https://presigned.example.com/get");
        verify(imageStorageService).confirmUpload(KEY);
        verify(dormitoryRepository).save(any(Dormitory.class));
        verify(imageStorageService, never()).delete(any());
    }

    @Test
    @DisplayName("거절됐던 기숙사 인증도 확정하면 PENDING으로 바뀐다")
    void confirmDormVerificationUpload_rejectedDormitory_resubmits() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "old-key");
        dormitory.reject("사진이 흐릿합니다");
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));
        when(imageStorageService.resolveViewUrl(KEY)).thenReturn("https://presigned.example.com/get");

        DormVerificationUploadResponse response =
                userService.confirmDormVerificationUpload(1L, new DormVerificationConfirmRequest(KEY));

        assertThat(response.dormStatus()).isEqualTo(DormStatus.PENDING.name());
        assertThat(dormitory.getDormitory()).isEqualTo("1기숙사");
        assertThat(dormitory.getRejectReason()).isNull();
        verify(dormitoryRepository, never()).save(any());
        verify(imageStorageService).delete("old-key");
    }

    @Test
    @DisplayName("본인 몫이 아닌 key면 확정이 거부되고 S3 조회조차 하지 않는다")
    void confirmDormVerificationUpload_keyNotOwned_throwsUserException() {
        assertThatThrownBy(() -> userService.confirmDormVerificationUpload(
                1L, new DormVerificationConfirmRequest(KEY_OTHER_USER)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.INVALID_DORM_VERIFICATION_IMAGE.getMessage());

        verify(imageStorageService, never()).confirmUpload(any());
        verify(dormitoryRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("확정 시점에 이미 심사 중이면 업로드된 객체를 지우고 거부한다(경쟁 상황 방지)")
    void confirmDormVerificationUpload_alreadyInProgress_deletesObjectAndThrows() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "existing-key");
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.of(dormitory));

        assertThatThrownBy(() -> userService.confirmDormVerificationUpload(
                1L, new DormVerificationConfirmRequest(KEY)))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.DORM_VERIFICATION_ALREADY_IN_PROGRESS.getMessage());

        verify(imageStorageService).delete(KEY);
        verify(imageStorageService, never()).confirmUpload(any());
    }

    @Test
    @DisplayName("실제로 업로드되지 않은 key면 확정이 거부된다(공용 이미지 모듈의 예외가 그대로 전파된다)")
    void confirmDormVerificationUpload_objectNotUploaded_throwsImageException() {
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());
        doThrow(new ImageException(ImageErrorCode.IMAGE_NOT_UPLOADED))
                .when(imageStorageService).confirmUpload(KEY);

        assertThatThrownBy(() -> userService.confirmDormVerificationUpload(
                1L, new DormVerificationConfirmRequest(KEY)))
                .isInstanceOf(ImageException.class)
                .hasMessage(ImageErrorCode.IMAGE_NOT_UPLOADED.getMessage());

        verify(dormitoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("업로드된 객체가 용량/형식 기준을 벗어나면 거부한다(삭제는 공용 모듈이 처리한다)")
    void confirmDormVerificationUpload_invalidUpload_throwsImageException() {
        when(dormitoryRepository.findByUserId(1L)).thenReturn(Optional.empty());
        doThrow(new ImageException(ImageErrorCode.IMAGE_TOO_LARGE))
                .when(imageStorageService).confirmUpload(KEY);

        assertThatThrownBy(() -> userService.confirmDormVerificationUpload(
                1L, new DormVerificationConfirmRequest(KEY)))
                .isInstanceOf(ImageException.class)
                .hasMessage(ImageErrorCode.IMAGE_TOO_LARGE.getMessage());

        verify(dormitoryRepository, never()).save(any());
    }

    // ===== withdraw =====

    @Test
    @DisplayName("비밀번호가 맞으면 탈퇴 처리(soft delete)되고 refresh token이 무효화된다")
    void withdraw_success() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(true);

        userService.withdraw(1L, new WithdrawalRequest("raw-pw"));

        assertThat(user.getStatus().name()).isEqualTo("DELETED");
        assertThat(user.getRefreshTokenHash()).isEmpty();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("유저를 찾을 수 없으면 USER_NOT_FOUND 예외가 발생한다")
    void withdraw_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.withdraw(1L, new WithdrawalRequest("raw-pw")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.USER_NOT_FOUND.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("BANNED 계정은 탈퇴할 수 없다")
    void withdraw_bannedAccount_throwsWithdrawalNotAllowed() {
        User banned = newUser();
        banned.ban();
        when(userRepository.findById(1L)).thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> userService.withdraw(1L, new WithdrawalRequest("raw-pw")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.WITHDRAWAL_NOT_ALLOWED.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 탈퇴가 거부된다")
    void withdraw_passwordMismatch() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pw", "encoded-pw")).thenReturn(false);

        assertThatThrownBy(() -> userService.withdraw(1L, new WithdrawalRequest("wrong-pw")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.PASSWORD_MISMATCH.getMessage());

        assertThat(user.getStatus().name()).isEqualTo("ACTIVE");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("방장으로 진행 중인(RECRUITING/CLOSED/ORDERED) 팟이 있으면 탈퇴가 거부된다")
    void withdraw_ongoingPartyAsCreator_throwsActivePotExists() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(true);
        when(deliveryPartyRepository.existsByCreatorIdAndStatusIn(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> userService.withdraw(1L, new WithdrawalRequest("raw-pw")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.ACTIVE_POT_EXISTS.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("방장이지만 COMPLETED인데 정산이 안 끝난 팟이 있으면 탈퇴가 거부된다")
    void withdraw_completedButUnsettledAsCreator_throwsUnsettledPotExists() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(true);
        when(deliveryPartyRepository.existsByCreatorIdAndStatusIn(eq(1L), any())).thenReturn(false);
        when(deliveryPartyRepository.existsByCreatorIdAndStatusAndSettlementStatusNotIn(
                eq(1L), eq(PartyStatus.COMPLETED), any())).thenReturn(true);

        assertThatThrownBy(() -> userService.withdraw(1L, new WithdrawalRequest("raw-pw")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.UNSETTLED_POT_EXISTS.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("참여자로 진행 중인(RECRUITING/CLOSED/ORDERED) 팟이 있으면 탈퇴가 거부된다")
    void withdraw_ongoingPartyAsParticipant_throwsActivePotExists() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(true);
        when(deliveryPartyRepository.existsByCreatorIdAndStatusIn(eq(1L), any())).thenReturn(false);
        PartyParticipant participation = new PartyParticipant(
                10L, 1L, PartyParticipantRole.MEMBER, PartyParticipantStatus.JOINED, LocalDateTime.now());
        when(partyParticipantRepository.findAllByUserIdAndStatus(1L, PartyParticipantStatus.JOINED))
                .thenReturn(List.of(participation));
        when(deliveryPartyRepository.existsByIdInAndStatusIn(eq(List.of(10L)), any())).thenReturn(true);

        assertThatThrownBy(() -> userService.withdraw(1L, new WithdrawalRequest("raw-pw")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.ACTIVE_POT_EXISTS.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("참여자로 속한 팟이 COMPLETED인데 정산이 안 끝났으면 탈퇴가 거부된다")
    void withdraw_completedButUnsettledAsParticipant_throwsUnsettledPotExists() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(true);
        when(deliveryPartyRepository.existsByCreatorIdAndStatusIn(eq(1L), any())).thenReturn(false);
        when(deliveryPartyRepository.existsByCreatorIdAndStatusAndSettlementStatusNotIn(
                eq(1L), eq(PartyStatus.COMPLETED), any())).thenReturn(false);
        PartyParticipant participation = new PartyParticipant(
                10L, 1L, PartyParticipantRole.MEMBER, PartyParticipantStatus.JOINED, LocalDateTime.now());
        when(partyParticipantRepository.findAllByUserIdAndStatus(1L, PartyParticipantStatus.JOINED))
                .thenReturn(List.of(participation));
        when(deliveryPartyRepository.existsByIdInAndStatusIn(eq(List.of(10L)), any())).thenReturn(false);
        when(deliveryPartyRepository.existsByIdInAndStatusAndSettlementStatusNotIn(
                eq(List.of(10L)), eq(PartyStatus.COMPLETED), any())).thenReturn(true);

        assertThatThrownBy(() -> userService.withdraw(1L, new WithdrawalRequest("raw-pw")))
                .isInstanceOf(UserException.class)
                .hasMessage(UserErrorCode.UNSETTLED_POT_EXISTS.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("참여 중인 팟이 없거나 모두 정산 완료/취소 상태면 탈퇴가 성공한다")
    void withdraw_noOngoingParty_succeeds() {
        User user = newUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-pw", "encoded-pw")).thenReturn(true);
        when(deliveryPartyRepository.existsByCreatorIdAndStatusIn(eq(1L), any())).thenReturn(false);
        when(deliveryPartyRepository.existsByCreatorIdAndStatusAndSettlementStatusNotIn(
                eq(1L), eq(PartyStatus.COMPLETED), any())).thenReturn(false);
        when(partyParticipantRepository.findAllByUserIdAndStatus(1L, PartyParticipantStatus.JOINED))
                .thenReturn(List.of());

        userService.withdraw(1L, new WithdrawalRequest("raw-pw"));

        assertThat(user.getStatus().name()).isEqualTo("DELETED");
        verify(userRepository).save(user);
    }
}