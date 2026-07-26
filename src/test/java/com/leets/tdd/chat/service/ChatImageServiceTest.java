package com.leets.tdd.chat.service;

import com.leets.tdd.chat.dto.ChatImageConfirmRequest;
import com.leets.tdd.chat.dto.ChatImagePresignRequest;
import com.leets.tdd.chat.dto.ChatImagePresignResponse;
import com.leets.tdd.global.storage.ImageCategory;
import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ChatImageServiceTest {

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private PartyParticipantRepository partyParticipantRepository;

    @InjectMocks
    private ChatImageService chatImageService;

    private static final Long PARTY_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String CHAT_KEY = "chat/1/1f0a2c4e-1b3d-4f5a-8c9d-0e1f2a3b4c5d.jpg";

    // 참여자 여부 stub 헬퍼
    private void givenParticipant(boolean joined) {
        when(partyParticipantRepository.existsByPartyIdAndUserIdAndStatus(
                PARTY_ID, USER_ID, PartyParticipantStatus.JOINED))
                .thenReturn(joined);
    }

    @Test
    @DisplayName("팟 참여자가 발급을 요청하면 key와 업로드 URL을 반환한다")
    void presign_participant_returnsUrl() {
        givenParticipant(true);
        when(imageStorageService.issueUploadUrl(eq(ImageCategory.CHAT), eq(PARTY_ID), anyString()))
                .thenReturn(new PresignedUploadResponse(CHAT_KEY, "https://s3.example.com/put", "image/jpeg", 300));

        ChatImagePresignResponse response =
                chatImageService.presignUpload(PARTY_ID, USER_ID, new ChatImagePresignRequest("image/jpeg"));

        assertThat(response.key()).isEqualTo(CHAT_KEY);
        assertThat(response.uploadUrl()).isEqualTo("https://s3.example.com/put");
        verify(imageStorageService).issueUploadUrl(eq(ImageCategory.CHAT), eq(PARTY_ID), anyString());
    }

    @Test
    @DisplayName("팟 참여자가 아니면 발급이 거부되고 S3를 호출하지 않는다")
    void presign_nonParticipant_throws() {
        givenParticipant(false);

        assertThatThrownBy(() ->
                chatImageService.presignUpload(PARTY_ID, USER_ID, new ChatImagePresignRequest("image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(imageStorageService, never()).issueUploadUrl(any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("팟 참여자가 확정을 요청하면 업로드 객체를 검증한다")
    void confirm_participant_verifiesUpload() {
        givenParticipant(true);

        chatImageService.confirmUpload(PARTY_ID, USER_ID, new ChatImageConfirmRequest(CHAT_KEY));

        verify(imageStorageService).confirmUpload(CHAT_KEY);
    }

    @Test
    @DisplayName("팟 참여자가 아니면 확정이 거부되고 검증을 호출하지 않는다")
    void confirm_nonParticipant_throws() {
        givenParticipant(false);

        assertThatThrownBy(() ->
                chatImageService.confirmUpload(PARTY_ID, USER_ID, new ChatImageConfirmRequest(CHAT_KEY)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(imageStorageService, never()).confirmUpload(anyString());
    }
}
