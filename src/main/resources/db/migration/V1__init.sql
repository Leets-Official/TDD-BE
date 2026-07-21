-- 초기 스키마. User, EmailVerificationCode 엔티티 기준.
-- 이후 스키마 변경은 ddl-auto로 하지 않고, 항상 새 V{n}__설명.sql 마이그레이션 파일을 추가해서 반영한다.

CREATE TABLE users
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE email_verification_codes
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    purpose     VARCHAR(255) NOT NULL,
    code        VARCHAR(255) NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    verified_at DATETIME(6)  NULL
);
