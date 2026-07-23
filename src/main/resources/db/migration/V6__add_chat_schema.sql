-- 채팅 도메인 스키마. ChatRoom, ChatMessage 엔티티 기준.
-- 채팅방은 배달팟에 1:1로 종속되며, 팟이 MATCHED 될 때 생성된다.

CREATE TABLE chat_rooms
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_id   BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_chat_rooms_party_id UNIQUE (party_id),
    CONSTRAINT fk_chat_rooms_party_id FOREIGN KEY (party_id) REFERENCES delivery_parties (id)
);

CREATE TABLE chat_messages
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id BIGINT       NOT NULL,
    sender_id    BIGINT       NULL,
    content      TEXT         NULL,
    message_type VARCHAR(30)  NOT NULL,
    image_url    VARCHAR(500) NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_chat_messages_chat_room_id FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id),
    CONSTRAINT fk_chat_messages_sender_id FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE INDEX idx_chat_messages_room_created_at ON chat_messages (chat_room_id, created_at);
