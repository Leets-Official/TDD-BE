ALTER TABLE users
    MODIFY COLUMN email VARCHAR(100) NOT NULL,
    MODIFY COLUMN password VARCHAR(100) NOT NULL,
    ADD COLUMN nickname VARCHAR(30) NOT NULL DEFAULT '',
    ADD COLUMN profile_image_url VARCHAR(500) NULL,
    ADD COLUMN manner_temperature DECIMAL(4, 1) NOT NULL DEFAULT 36.5,
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

CREATE TABLE food_categories
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_food_categories_name UNIQUE (name)
);

CREATE TABLE bank_accounts
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    bank_name      VARCHAR(30) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    account_holder VARCHAR(30) NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_bank_accounts_user_id UNIQUE (user_id),
    CONSTRAINT fk_bank_accounts_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE delivery_parties
(
    id                         BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id                 BIGINT NOT NULL,
    food_category_id           BIGINT NOT NULL,
    title                      VARCHAR(100) NOT NULL,
    description                TEXT NULL,
    min_participants           INT NOT NULL DEFAULT 2,
    max_participants           INT NOT NULL,
    order_expected_at          DATETIME(6) NOT NULL,
    status                     VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
    closed_at                  DATETIME(6) NULL,
    settlement_status          VARCHAR(20) NOT NULL DEFAULT 'NONE',
    settlement_total_amount    INT NULL,
    settlement_requested_at    DATETIME(6) NULL,
    settlement_bank_account_id BIGINT NULL,
    created_at                 DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                 DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_delivery_parties_creator_id FOREIGN KEY (creator_id) REFERENCES users (id),
    CONSTRAINT fk_delivery_parties_food_category_id FOREIGN KEY (food_category_id) REFERENCES food_categories (id),
    CONSTRAINT fk_delivery_parties_settlement_bank_account_id FOREIGN KEY (settlement_bank_account_id) REFERENCES bank_accounts (id)
);

CREATE TABLE party_participants
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_id          BIGINT NOT NULL,
    user_id           BIGINT NOT NULL,
    role              VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    joined_at         DATETIME(6) NOT NULL,
    canceled_at       DATETIME(6) NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'JOINED',
    settlement_amount INT NULL,
    payment_status    VARCHAR(20) NULL,
    paid_at           DATETIME(6) NULL,
    CONSTRAINT uk_party_participants_party_user UNIQUE (party_id, user_id),
    CONSTRAINT fk_party_participants_party_id FOREIGN KEY (party_id) REFERENCES delivery_parties (id),
    CONSTRAINT fk_party_participants_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE review_tags
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    category   VARCHAR(10) NOT NULL,
    label      VARCHAR(50) NOT NULL,
    content    VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_review_tags_category_label UNIQUE (category, label)
);

CREATE TABLE reviews
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_id    BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    rating      INT NOT NULL,
    content     TEXT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_reviews_party_reviewer_reviewee UNIQUE (party_id, reviewer_id, reviewee_id),
    CONSTRAINT fk_reviews_party_id FOREIGN KEY (party_id) REFERENCES delivery_parties (id),
    CONSTRAINT fk_reviews_reviewer_id FOREIGN KEY (reviewer_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_reviewee_id FOREIGN KEY (reviewee_id) REFERENCES users (id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE review_tag_mappings
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id     BIGINT NOT NULL,
    review_tag_id BIGINT NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_review_tag_mappings_review_tag UNIQUE (review_id, review_tag_id),
    CONSTRAINT fk_review_tag_mappings_review_id FOREIGN KEY (review_id) REFERENCES reviews (id),
    CONSTRAINT fk_review_tag_mappings_review_tag_id FOREIGN KEY (review_tag_id) REFERENCES review_tags (id)
);

CREATE TABLE reports
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_id         BIGINT NOT NULL,
    reporter_id      BIGINT NOT NULL,
    reported_user_id BIGINT NOT NULL,
    reason           VARCHAR(30) NOT NULL,
    content          TEXT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reports_party_id FOREIGN KEY (party_id) REFERENCES delivery_parties (id),
    CONSTRAINT fk_reports_reporter_id FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT fk_reports_reported_user_id FOREIGN KEY (reported_user_id) REFERENCES users (id)
);

CREATE INDEX idx_party_participants_user_id ON party_participants (user_id);
CREATE INDEX idx_reviews_reviewee_created_at ON reviews (reviewee_id, created_at DESC);
CREATE INDEX idx_reports_reported_user_id ON reports (reported_user_id);
