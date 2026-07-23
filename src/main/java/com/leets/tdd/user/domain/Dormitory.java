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

import java.time.LocalDateTime;

/**
 * 기숙사 인증 정보. 유저당 row 1개만 유지한다(user_id unique).
 * "미인증"(기숙사 관련 정보를 아예 입력한 적 없음)은 이 엔티티의 row 자체가 없는 상태로 표현한다.
 * 동만 입력하고 인증 사진은 아직 제출하지 않은 경우 row는 생기지만 상태는 NOT_SUBMITTED이고,
 * 사진까지 제출해야 PENDING(심사 대기)으로 바뀐다.
 * dormVerifiedAt은 "승인된 시각"만을 의미한다. approve() 될 때만 채워지고,
 * 그 외 상태에서는 null이다(승인된 적이 없거나 재신청한 경우).
 */
@Entity
@Getter
@Table(name = "dormitory")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dormitory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 20)
    private String dormitory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DormStatus dormStatus;

    @Column
    private LocalDateTime dormVerifiedAt;

    @Column
    private LocalDateTime dormVerifiedUntil;

    @Column(length = 500)
    private String dormVerificationImageKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String rejectReason;

    public Dormitory(Long userId, String dormitory, String dormVerificationImageKey) {
        this.userId = userId;
        this.dormitory = dormitory;
        this.dormVerificationImageKey = dormVerificationImageKey;
        this.dormStatus = initialStatus(dormVerificationImageKey);
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    private static DormStatus initialStatus(String dormVerificationImageKey) {
        return dormVerificationImageKey != null ? DormStatus.PENDING : DormStatus.NOT_SUBMITTED;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(LocalDateTime until) {
        this.dormStatus = DormStatus.APPROVED;
        this.dormVerifiedAt = LocalDateTime.now();
        this.dormVerifiedUntil = until;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        this.dormStatus = DormStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void expire() {
        this.dormStatus = DormStatus.EXPIRED;
    }

    public void resubmit(String dormitory, String dormVerificationImageKey) {
        this.dormitory = dormitory;
        this.dormVerificationImageKey = dormVerificationImageKey;
        this.dormStatus = initialStatus(dormVerificationImageKey);
        this.dormVerifiedAt = null;
        this.dormVerifiedUntil = null;
        this.rejectReason = null;
    }
}
