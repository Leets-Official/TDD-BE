package com.leets.tdd.user.service;

import com.leets.tdd.global.jwt.JwtProvider;
import com.leets.tdd.global.jwt.RefreshTokenHasher;
import com.leets.tdd.auth.service.EmailVerificationService;
import com.leets.tdd.user.domain.DormStatus;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.dto.ProfileRegistrationRequest;
import com.leets.tdd.user.dto.ProfileRegistrationResponse;
import com.leets.tdd.user.dto.ProfileUpdateRequest;
import com.leets.tdd.user.dto.ProfileUpdateResponse;
import com.leets.tdd.user.dto.PushSettingRequest;
import com.leets.tdd.user.dto.PushSettingResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        ProfileUpdateResponse response = userService.updateProfile(
                1L, updateRequest("새닉네임", "2기숙사", "https://img.example.com/a.png"));

        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.dormitory()).isEqualTo("2기숙사");
        assertThat(response.profileImageUrl()).isEqualTo("https://img.example.com/a.png");
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
}
