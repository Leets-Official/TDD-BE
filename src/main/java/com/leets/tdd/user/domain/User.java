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

    public void liftSuspension() {
        this.status = UserStatus.ACTIVE;
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
}
