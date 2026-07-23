package com.leets.tdd.chat.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    // 발신자 ID. 시스템 메시지는 null
    @Column(name = "sender_id")
    private Long senderId;

    // 메시지 내용. 이미지만 보낼 땐 null 가능
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    // 이미지 메시지일 때 사진 URL
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 일반 사용자 메시지 생성
    public static ChatMessage createUserMessage(Long chatRoomId, Long senderId, String content) {
        ChatMessage message = new ChatMessage();
        message.chatRoomId = chatRoomId;
        message.senderId = senderId;
        message.content = content;
        message.messageType = MessageType.USER;
        return message;
    }

    // 이미지 메시지 생성
    public static ChatMessage createImageMessage(Long chatRoomId, Long senderId, String imageUrl) {
        ChatMessage message = new ChatMessage();
        message.chatRoomId = chatRoomId;
        message.senderId = senderId;
        message.imageUrl = imageUrl;
        message.messageType = MessageType.IMAGE;
        return message;
    }

    // 시스템 메시지 생성 (senderId 없음)
    public static ChatMessage createSystemMessage(Long chatRoomId, MessageType type, String content) {
        ChatMessage message = new ChatMessage();
        message.chatRoomId = chatRoomId;
        message.content = content;
        message.messageType = type;
        return message;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
