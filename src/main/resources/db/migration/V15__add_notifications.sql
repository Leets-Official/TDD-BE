-- 알림 이력 스키마. Notification 엔티티 기준.
-- 각 도메인 알림 트리거(채팅/게시판/배달팟/정산)가 웹푸시 발송과 함께 이 테이블에 알림 이력을 남긴다.
-- 웹푸시 구독 여부와 무관하게 "해당 유저에게 이런 알림이 발생했다"는 사실을 기록한다.
-- 조회 API는 알림 목록 화면 도입 시 별도로 추가한다(현재는 저장 전용).
-- is_read는 향후 읽음 처리/화면용으로 컬럼만 미리 둔다.
CREATE TABLE notifications
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(100) NOT NULL,
    body       VARCHAR(255) NOT NULL,
    category   VARCHAR(30)  NOT NULL,
    url        VARCHAR(500) NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_notifications_user_created_at ON notifications (user_id, created_at DESC);
