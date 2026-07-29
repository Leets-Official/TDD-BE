package com.leets.tdd.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.leets.tdd.chat.domain.ChatMessage;
import com.leets.tdd.chat.domain.ChatRoom;
import com.leets.tdd.chat.domain.MessageType;
import com.leets.tdd.chat.repository.ChatMessageRepository;
import com.leets.tdd.chat.repository.ChatRoomRepository;
import com.leets.tdd.global.webpush.WebPushPayload;
import com.leets.tdd.global.webpush.WebPushSender;
import com.leets.tdd.settlement.domain.BankAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class SettlementNotifierTest {

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private WebPushSender webPushSender;

  @InjectMocks
  private SettlementNotifier settlementNotifier;

  @Test
  void 정산_요청_시스템_메시지에_총액과_계좌를_담아_저장하고_브로드캐스트한다() {
    ChatRoom chatRoom = chatRoom(77L);
    BankAccount account = bankAccount();
    given(chatRoomRepository.findByPartyId(10L)).willReturn(Optional.of(chatRoom));
    given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(call -> call.getArgument(0));

    settlementNotifier.notifySettlementRequested(10L, 20_000, account);

    ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
    verify(chatMessageRepository).save(captor.capture());
    ChatMessage saved = captor.getValue();
    assertThat(saved.getMessageType()).isEqualTo(MessageType.SETTLEMENT_REQUEST);
    assertThat(saved.getChatRoomId()).isEqualTo(77L);
    assertThat(saved.getSenderId()).isNull();
    assertThat(saved.getContent())
        .contains("총 20,000원")
        .contains("우리은행 1002123456789 (강지훈)");
    verify(messagingTemplate).convertAndSend(Mockito.eq("/topic/parties/10/chat"), Mockito.<Object>any());
  }

  @Test
  void 채팅방이_없으면_시스템_메시지를_저장하지_않는다() {
    given(chatRoomRepository.findByPartyId(10L)).willReturn(Optional.empty());

    settlementNotifier.notifySettlementRequested(10L, 20_000, Mockito.mock(BankAccount.class));

    verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    verify(messagingTemplate, never()).convertAndSend(anyString(), Mockito.<Object>any());
  }

  @Test
  void 브로드캐스트가_실패해도_정산_흐름으로_예외를_전파하지_않는다() {
    ChatRoom chatRoom = chatRoom(77L);
    BankAccount account = bankAccount();
    given(chatRoomRepository.findByPartyId(10L)).willReturn(Optional.of(chatRoom));
    given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(call -> call.getArgument(0));
    willThrow(new IllegalStateException("broker down"))
        .given(messagingTemplate).convertAndSend(anyString(), Mockito.<Object>any());

    assertThatCode(() -> settlementNotifier.notifySettlementRequested(10L, 20_000, account))
        .doesNotThrowAnyException();

    verify(chatMessageRepository).save(any(ChatMessage.class));
  }

  @Test
  void 정산_완료_알림을_배달팟_카테고리로_발송한다() {
    settlementNotifier.notifySettlementCompleted(10L, "치킨 같이 시켜요", List.of(2L, 3L));

    ArgumentCaptor<WebPushPayload> captor = ArgumentCaptor.forClass(WebPushPayload.class);
    verify(webPushSender).sendToUsers(Mockito.eq(List.of(2L, 3L)), captor.capture());
    WebPushPayload payload = captor.getValue();
    assertThat(payload.category()).isEqualTo("POT");
    assertThat(payload.body()).contains("치킨 같이 시켜요");
    assertThat(payload.url()).isEqualTo("/parties/10");
  }

  @Test
  void 정산_대상이_없으면_웹푸시를_보내지_않는다() {
    settlementNotifier.notifySettlementCompleted(10L, "치킨 같이 시켜요", List.of());

    verify(webPushSender, never()).sendToUsers(any(), any());
  }

  private ChatRoom chatRoom(Long id) {
    ChatRoom chatRoom = Mockito.mock(ChatRoom.class);
    given(chatRoom.getId()).willReturn(id);
    return chatRoom;
  }

  private BankAccount bankAccount() {
    BankAccount account = Mockito.mock(BankAccount.class);
    given(account.getBankName()).willReturn("우리은행");
    given(account.getAccountNumber()).willReturn("1002123456789");
    given(account.getAccountHolder()).willReturn("강지훈");
    return account;
  }
}
