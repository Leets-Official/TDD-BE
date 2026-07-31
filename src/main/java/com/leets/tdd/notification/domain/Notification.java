package com.leets.tdd.notification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림을 받는 사용자
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, length = 255)
    private String body;

    // 알림 분류: POT / CHAT / BOARD / SETTLEMENT
    @Column(name = "category", nullable = false, length = 30)
    private String category;

    // 클릭 시 이동할 경로 (선택)
    @Column(name = "url", length = 500)
    private String url;

    // 읽음 여부. 읽음 처리/알림 화면은 후속 안건이며 현재는 기본값(false)으로만 저장한다.
    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Notification create(Long userId, String title, String body, String category, String url) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.title = title;
        notification.body = body;
        notification.category = category;
        notification.url = url;
        notification.isRead = false;
        return notification;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
