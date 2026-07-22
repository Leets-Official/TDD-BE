-- ERD 반영: 회원가입 완료(/users/me), 마이페이지, 노쇼 페널티, 기숙사 인증 기능 지원.
-- 주의: users에 NOT NULL 컬럼을 다수 추가하면서 기존 row가 있을 경우를 대비해 DEFAULT를 넣었다.
-- nickname은 처음엔 NULL 허용으로 추가해서 기존 row에 겹치지 않는 값으로 백필한 다음에야
-- NOT NULL + UNIQUE 제약을 건다(기존 row가 2개 이상이어도 안전하게 적용되도록).

ALTER TABLE users
    ADD COLUMN nickname VARCHAR(30) NULL,
    ADD COLUMN profile_image_url VARCHAR(500) NULL,
    ADD COLUMN manner_temperature DECIMAL(4, 1) NOT NULL DEFAULT 36.5,
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN status VARCHAR(100) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN no_show_approved_count INT NOT NULL DEFAULT 0,
    ADD COLUMN suspended_until DATETIME(6) NULL,
    ADD COLUMN refresh_token_hash VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN refresh_token_expires_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN push_endpoint VARCHAR(500) NULL,
    ADD COLUMN push_p256dh_key VARCHAR(255) NULL,
    ADD COLUMN push_auth_key VARCHAR(255) NULL,
    ADD COLUMN push_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE users
SET nickname = CONCAT('user_', id)
WHERE nickname IS NULL OR nickname = '';

ALTER TABLE users
    MODIFY COLUMN nickname VARCHAR(30) NOT NULL,
    ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);

CREATE TABLE dormitory
(
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                     BIGINT       NOT NULL,
    dormitory                   VARCHAR(20)  NULL,
    dorm_status                 VARCHAR(20)  NOT NULL,
    dorm_verified_at            DATETIME(6)  NULL,
    dorm_verified_until         DATETIME(6)  NULL,
    dorm_verification_image_key VARCHAR(500) NULL,
    created_at                  DATETIME(6)  NOT NULL,
    updated_at                  DATETIME(6)  NOT NULL,
    reject_reason                VARCHAR(100) NULL,
    CONSTRAINT uk_dormitory_user_id UNIQUE (user_id),
    CONSTRAINT fk_dormitory_user FOREIGN KEY (user_id) REFERENCES users (id)
);
