package com.leets.tdd.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 회원 엔티티.
 * - 노쇼 3회 -> 7일 정지, 5회 -> 10일 정지, 8회 -> 영구 제한(BANNED)
 * - SUSPENDED는 배달 팟 개설/참여만 막고 로그인 등 나머지는 정상 이용 가능
 * - BANNED는 로그인 자체를 차단하고, 탈퇴(DELETED 전환) 자체가 불가능하다
 * - 탈퇴(DELETED)한 계정이 같은 이메일로 재가입하면 이 row를 재사용(reactivate)한다.
 *   이때 noShowApprovedCount/suspendedUntil/mannerTemperature는 유지한다.
 *   (정지 기간이 남아있는 상태에서 탈퇴 후 즉시 재가입해서 정지를 우회하는 것을 막기 위함)
 */
@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private static final BigDecimal DEFAULT_MANNER_TEMPERATURE = new BigDecimal("36.5");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal mannerTemperature;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private UserStatus status;

    @Column(nullable = false)
    private int noShowApprovedCount;

    @Column
    private LocalDateTime suspendedUntil;

    @Column(nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(nullable = false)
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "push_endpoint", length = 500)
    private String pushEndpoint;

    @Column(name = "push_p256dh_key", length = 255)
    private String pushP256dhKey;

    @Column(name = "push_auth_key", length = 255)
    private String pushAuthKey;

    @Column(nullable = false)
    private boolean pushEnabled;

    @Column(nullable = false)
    private int failedLoginAttempts;

    @Column
    private LocalDateTime lastFailedLoginAt;

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    // AuthService가 원자적 UPDATE(UserRepository.recordFailedLogin)의 windowStart 파라미터를
    // 계산할 때도 같은 값을 써야 해서 public으로 둔다.
    public static final Duration LOGIN_ATTEMPT_WINDOW = Duration.ofMinutes(5);
    private static final Duration LOGIN_BLOCK_DURATION = Duration.ofMinutes(15);

    public User(String email, String nickname, String password,
                 String refreshTokenHash, LocalDateTime refreshTokenExpiresAt) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.mannerTemperature = DEFAULT_MANNER_TEMPERATURE;
        this.status = UserStatus.ACTIVE;
        this.noShowApprovedCount = 0;
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
        this.pushEnabled = true;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // develop에서 정산/후기 기능(ReviewServiceImpl 등)이 (email, password, nickname) 순서로
    // 만들던 생성자. 우리 쪽 생성자로 그대로 위임해서 나머지 필드는 기본값으로 채운다.
    public User(String email, String password, String nickname) {
        this(email, nickname, password, "", LocalDateTime.now());
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseNoShowCount() {
        this.noShowApprovedCount++;
    }

    public void suspend(LocalDateTime until) {
        this.status = UserStatus.SUSPENDED;
        this.suspendedUntil = until;
    }

    public void ban() {
        this.status = UserStatus.BANNED;
        this.suspendedUntil = null;
    }


    //정지 기간이 지났는데도 status가 아직 SUSPENDED로 남아있는지 확인한다(lazy check용).
    //이 값이 true일 때만 liftSuspension()으로 ACTIVE로 되돌려야 한다.

    public boolean isSuspensionExpired() {
        return status == UserStatus.SUSPENDED
                && suspendedUntil != null
                && LocalDateTime.now().isAfter(suspendedUntil);
    }

    // 정지 기간이 자연히 지나서 ACTIVE로 되돌리는 경우라, 정지 우회 방지용으로 남겨둘 이유가
    // 없다(탈퇴 시 우회 방지는 softDelete() 경로에서 별도로 값을 세팅해서 처리한다).
    // 여기서 지워두지 않으면 이미 지난 시각이 MyPageResponse.suspendedUntil로 그대로 노출된다.
    public void liftSuspension() {
        this.status = UserStatus.ACTIVE;
        this.suspendedUntil = null;
    }


    //ACTIVE/SUSPENDED만 탈퇴 가능하고 BANNED는 불가능하다.
    public boolean canWithdraw() {
        return status == UserStatus.ACTIVE || status == UserStatus.SUSPENDED;
    }


    //탈퇴 처리. suspendedUntil은 의도적으로 지우지 않는다(정지 우회 방지).
    public void softDelete() {
        this.status = UserStatus.DELETED;
    }


    //탈퇴 상태인데도 아직 정지 기간이 남아있는지(재가입 차단 여부) 확인한다.
    public boolean isWithinSuspensionPeriod() {
        return suspendedUntil != null && LocalDateTime.now().isBefore(suspendedUntil);
    }


    //탈퇴했던 계정을 같은 이메일로 재가입시킬 때 재사용한다.
    //noShowApprovedCount/suspendedUntil/mannerTemperature는 유지, 나머지 프로필성 정보는 초기화한다.
    public void reactivate(String nickname, String password,
                            String refreshTokenHash, LocalDateTime refreshTokenExpiresAt) {
        this.status = UserStatus.ACTIVE;
        this.nickname = nickname;
        this.password = password;
        this.profileImageUrl = null;
        this.pushEndpoint = null;
        this.pushP256dhKey = null;
        this.pushAuthKey = null;
        this.pushEnabled = true;
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    public void updateRefreshToken(String refreshTokenHash, LocalDateTime refreshTokenExpiresAt) {
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    }

    // 로그아웃 시 refresh token을 무효화한다. refreshTokenHash는 not-null 컬럼이라 null 대신
    // RefreshTokenCleanupScheduler와 같은 관례로 빈 문자열을 "토큰 없음" 상태로 쓴다.
    // refreshTokenExpiresAt은 그대로 둬도 되는데, 어차피 재발급 시 해시 일치 여부부터 확인해서
    // 빈 문자열과는 절대 일치할 수 없기 때문이다.
    public void clearRefreshToken() {
        this.refreshTokenHash = "";
    }

    // develop의 정산/후기 기능(ReviewServiceImpl)에서 매너온도 갱신에 사용.
    // updatedAt은 @PreUpdate가 flush 시 자동으로 갱신해주니 여기서 따로 안 건드림.
    public void updateMannerTemperature(BigDecimal delta) {
        this.mannerTemperature = this.mannerTemperature.add(delta);
    }

    // 로그인 시도 제한 확인용(5분 내 3회 실패 시 15분 차단).
    // lastFailedLoginAt 하나로 "실패 3회 미만일 때의 5분 리셋 판단"과
    // "실패 3회 이상일 때의 15분 차단 판단"을 둘 다 계산한다(별도 만료시각 컬럼 없이).
    public boolean isLoginBlocked() {
        return failedLoginAttempts >= MAX_LOGIN_ATTEMPTS
                && lastFailedLoginAt != null
                && LocalDateTime.now().isBefore(lastFailedLoginAt.plus(LOGIN_BLOCK_DURATION));
    }

    // 비밀번호 불일치로 로그인에 실패했을 때의 카운트 증가는 여기(엔티티 메서드 + save)가 아니라
    // UserRepository.recordFailedLogin()의 원자적 UPDATE로 처리한다. 같은 유저에게 동시에 여러
    // 실패 요청이 오면 "읽고-메모리에서 증가시키고-저장"하는 방식은 두 요청이 같은 값을 읽어서
    // 하나가 유실되는 race condition이 생기기 때문이다(3회 제한이 동시요청으로 우회될 수 있음).

    // 로그인에 성공했을 때 실패 카운트/차단 상태를 초기화한다.
    public void resetLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
    }
}
