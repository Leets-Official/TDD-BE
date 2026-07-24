package com.leets.tdd.chat.repository;

import com.leets.tdd.chat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채팅방의 메시지를 최신순으로 가져온다.
    // createdAt이 같은 메시지가 있을 수 있으므로 id를 보조 기준으로 두어 순서를 고정한다.
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtDescIdDesc(Long chatRoomId, Pageable pageable);
}
