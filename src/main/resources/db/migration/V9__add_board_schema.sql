-- 기숙사 게시판 스키마. Post, Comment 엔티티 기준.
-- scope/dormitory는 동별 게시판 구분용으로 컬럼만 미리 두고, MVP에서는 전체 조회로만 사용한다.

CREATE TABLE posts
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    scope      VARCHAR(10)  NOT NULL DEFAULT 'ALL',
    dormitory  VARCHAR(20)  NULL,
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_posts_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE comments
(
    id                BIGINT      AUTO_INCREMENT PRIMARY KEY,
    post_id           BIGINT      NOT NULL,
    user_id           BIGINT      NOT NULL,
    parent_comment_id BIGINT      NULL,
    content           TEXT        NOT NULL,
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_comments_post_id FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_comments_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_comments_parent_comment_id FOREIGN KEY (parent_comment_id) REFERENCES comments (id)
);

CREATE INDEX idx_posts_created_at ON posts (created_at DESC);
CREATE INDEX idx_comments_post_created_at ON comments (post_id, created_at);