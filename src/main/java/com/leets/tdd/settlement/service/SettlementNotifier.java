package com.leets.tdd.settlement.service;

import com.leets.tdd.chat.domain.ChatMessage;
import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.domain.MessageType;
import com.leets.tdd.chat.dto.ChatMessageResponse;
import com.leets.tdd.chat.repository.ChatMessageRepository;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import com.leets.tdd.global.webpush.WebPushPayload;
import com.leets.tdd.global.webpush.WebPushSender;
import com.leets.tdd.settlement.domain.BankAccount;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementNotifier {

  private static final String CHAT_TOPIC_FORMAT = "/topic/parties/%d/chat";
  private static final String PARTY_CATEGORY = "POT";
  private static final String PARTY_URL_FORMAT = "/parties/%d";

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final WebPushSender webPushSender;

  public void notifySettlementRequested(Long partyId, Integer totalAmount, BankAccount bankAccount) {
    ChatRoom chatRoom = chatRoomRepository.findByPartyId(partyId).orElse(null);
    if (chatRoom == null) {
      log.warn("settlement.notify.no_chat_room partyId={}", partyId);
      return;
    }

    ChatMessage message = chatMessageRepository.save(ChatMessage.createSystemMessage(
        chatRoom.getId(),
        MessageType.SETTLEMENT_REQUEST,
        settlementRequestContent(totalAmount, bankAccount)));

    broadcast(partyId, ChatMessageResponse.from(message));
  }

  public void notifySettlementCompleted(Long partyId, String partyTitle, List<Long> targetUserIds) {
    if (targetUserIds.isEmpty()) {
      return;
    }
    webPushSender.sendToUsers(targetUserIds, new WebPushPayload(
        "정산이 완료되었어요",
        "%s 정산이 마무리되었습니다.".formatted(partyTitle),
        PARTY_CATEGORY,
        PARTY_URL_FORMAT.formatted(partyId)));
  }

  private String settlementRequestContent(Integer totalAmount, BankAccount bankAccount) {
    return """
        정산이 요청되었습니다.
        총 %,d원
        %s %s (%s)""".formatted(
        totalAmount,
        bankAccount.getBankName(),
        bankAccount.getAccountNumber(),
        bankAccount.getAccountHolder());
  }

  private void broadcast(Long partyId, ChatMessageResponse message) {
    try {
      messagingTemplate.convertAndSend(CHAT_TOPIC_FORMAT.formatted(partyId), message);
    } catch (Exception e) {
      log.warn("settlement.notify.broadcast_failed partyId={}", partyId, e);
    }
  }
}
