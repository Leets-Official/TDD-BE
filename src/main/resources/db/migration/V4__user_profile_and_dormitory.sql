-- ERD 반영: 회원가입 완료(/users/me), 마이페이지, 노쇼 페널티, 기숙사 인증 기능 지원.
-- 주의: users에 NOT NULL 컬럼을 다수 추가하면서 기존 row가 있을 경우를 대비해 DEFAULT를 넣었다.
-- nickname/profile_image_url/manner_temperature/created_at/updated_at은 V3(정산/후기 기능,
-- V3__add_settlement_and_review_schema.sql)에서 이미 users에 추가했으므로 여기서는 다시
-- 추가하지 않는다(중복 컬럼 에러 방지). nickname은 V3에서 NOT NULL DEFAULT ''로만 들어가고
-- UNIQUE 제약은 없으므로, 기존 row에 겹치지 않는 값으로 백필한 다음 여기서 UNIQUE만 추가한다.
-- ALTER TABLE 하나에 ADD COLUMN을 콤마로 여러 개 묶는 MySQL 문법은 테스트용 H2(MODE=MySQL)
-- 에서 파싱이 안 돼서, 컬럼/제약 하나당 ALTER TABLE 문을 하나씩 따로 쓴다.

ALTER TABLE users ADD COLUMN status VARCHAR(100) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN no_show_approved_count INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN suspended_until DATETIME(6) NULL;
ALTER TABLE users ADD COLUMN refresh_token_hash VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN refresh_token_expires_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE users ADD COLUMN push_endpoint VARCHAR(500) NULL;
ALTER TABLE users ADD COLUMN push_p256dh_key VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN push_auth_key VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN push_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE users
SET nickname = CONCAT('user_', id)
WHERE nickname IS NULL OR nickname = '';

ALTER TABLE users MODIFY COLUMN nickname VARCHAR(30) NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_nickname UNIQUE (nickname);

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
