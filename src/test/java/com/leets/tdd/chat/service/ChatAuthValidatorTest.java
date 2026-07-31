package com.leets.tdd.chat.service;

import com.leets.tdd.party.domain.DeliveryParty;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.domain.PartyStatus;
import com.leets.tdd.party.repository.DeliveryPartyRepository;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAuthValidatorTest {

    @Mock
    private DeliveryPartyRepository deliveryPartyRepository;

    @Mock
    private PartyParticipantRepository partyParticipantRepository;

    @InjectMocks
    private ChatAuthValidator chatAuthValidator;

    private static final Long PARTY_ID = 1L;
    private static final Long CREATOR_ID = 10L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long OUTSIDER_ID = 30L;

    // creatorId와 status를 가진 DeliveryParty를 만든다
    private DeliveryParty party(Long creatorId, PartyStatus status) {
        try {
            java.lang.reflect.Constructor<DeliveryParty> constructor =
                    DeliveryParty.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            DeliveryParty party = constructor.newInstance();
            ReflectionTestUtils.setField(party, "id", PARTY_ID);
            ReflectionTestUtils.setField(party, "creatorId", creatorId);
            ReflectionTestUtils.setField(party, "status", status);
            return party;
        } catch (Exception e) {
            throw new RuntimeException("테스트용 DeliveryParty 생성 실패", e);
        }
    }

    @Test
    @DisplayName("팟의 방장은 참여자 row가 없어도 채팅 접근이 허용된다")
    void validateChatAccess_creator_allowed() {
        when(deliveryPartyRepository.findById(PARTY_ID))
                .thenReturn(Optional.of(party(CREATOR_ID, PartyStatus.CLOSED)));

        assertThatCode(() -> chatAuthValidator.validateChatAccess(PARTY_ID, CREATOR_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("JOINED 참여자는 채팅 접근이 허용된다")
    void validateChatAccess_participant_allowed() {
        when(deliveryPartyRepository.findById(PARTY_ID))
                .thenReturn(Optional.of(party(CREATOR_ID, PartyStatus.CLOSED)));
        when(partyParticipantRepository
                .existsByPartyIdAndUserIdAndStatus(PARTY_ID, PARTICIPANT_ID, PartyParticipantStatus.JOINED))
                .thenReturn(true);

        assertThatCode(() -> chatAuthValidator.validateChatAccess(PARTY_ID, PARTICIPANT_ID))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("방장도 참여자도 아니면 채팅 접근이 거부된다")
    void validateChatAccess_outsider_denied() {
        when(deliveryPartyRepository.findById(PARTY_ID))
                .thenReturn(Optional.of(party(CREATOR_ID, PartyStatus.CLOSED)));
        when(partyParticipantRepository
                .existsByPartyIdAndUserIdAndStatus(PARTY_ID, OUTSIDER_ID, PartyParticipantStatus.JOINED))
                .thenReturn(false);

        assertThatThrownBy(() -> chatAuthValidator.validateChatAccess(PARTY_ID, OUTSIDER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("취소된 팟에서는 방장이어도 채팅 접근이 거부된다")
    void validateChatAccess_canceledParty_denied() {
        when(deliveryPartyRepository.findById(PARTY_ID))
                .thenReturn(Optional.of(party(CREATOR_ID, PartyStatus.CANCELED)));

        assertThatThrownBy(() -> chatAuthValidator.validateChatAccess(PARTY_ID, CREATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료된 배달팟에서는 채팅을 이용할 수 없습니다.");
    }

    @Test
    @DisplayName("완료된 팟에서는 참여자여도 채팅 접근이 거부된다")
    void validateChatAccess_completedParty_denied() {
        when(deliveryPartyRepository.findById(PARTY_ID))
                .thenReturn(Optional.of(party(CREATOR_ID, PartyStatus.COMPLETED)));

        assertThatThrownBy(() -> chatAuthValidator.validateChatAccess(PARTY_ID, PARTICIPANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("종료된 배달팟에서는 채팅을 이용할 수 없습니다.");
    }
}
