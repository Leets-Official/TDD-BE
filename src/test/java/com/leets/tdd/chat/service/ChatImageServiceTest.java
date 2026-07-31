package com.leets.tdd.chat.service;

import com.leets.tdd.chat.dto.ChatImageConfirmRequest;
import com.leets.tdd.chat.dto.ChatImagePresignRequest;
import com.leets.tdd.chat.dto.ChatImagePresignResponse;
import com.leets.tdd.global.storage.ImageCategory;
import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ChatImageServiceTest {

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private ChatAuthValidator chatAuthValidator;

    @InjectMocks
    private ChatImageService chatImageService;

    private static final Long PARTY_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final String CHAT_KEY = "chat/1/1f0a2c4e-1b3d-4f5a-8c9d-0e1f2a3b4c5d.jpg";

    // 채팅 접근 권한이 없을 때 validator가 예외를 던지도록 stub
    private void givenNoChatAccess() {
        doThrow(new IllegalArgumentException("해당 팟의 참여자만 채팅에 접근할 수 있습니다."))
                .when(chatAuthValidator).validateChatAccess(PARTY_ID, USER_ID);
    }

    @Test
    @DisplayName("채팅 접근 권한이 있으면 발급이 진행되어 key와 업로드 URL을 반환한다")
    void presign_authorized_returnsUrl() {
        when(imageStorageService.issueUploadUrl(eq(ImageCategory.CHAT), eq(PARTY_ID), eq("image/jpeg")))
                .thenReturn(new PresignedUploadResponse(CHAT_KEY, "https://s3.example.com/put", "image/jpeg", 300));

        ChatImagePresignResponse response =
                chatImageService.presignUpload(PARTY_ID, USER_ID, new ChatImagePresignRequest("image/jpeg"));

        assertThat(response.key()).isEqualTo(CHAT_KEY);
        assertThat(response.uploadUrl()).isEqualTo("https://s3.example.com/put");
        verify(chatAuthValidator).validateChatAccess(PARTY_ID, USER_ID);
        verify(imageStorageService).issueUploadUrl(eq(ImageCategory.CHAT), eq(PARTY_ID), eq("image/jpeg"));
    }

    @Test
    @DisplayName("채팅 접근 권한이 없으면 발급이 거부되고 S3를 호출하지 않는다")
    void presign_unauthorized_throws() {
        givenNoChatAccess();

        assertThatThrownBy(() ->
                chatImageService.presignUpload(PARTY_ID, USER_ID, new ChatImagePresignRequest("image/jpeg")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(imageStorageService, never()).issueUploadUrl(any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("채팅 접근 권한이 있으면 확정이 진행되어 업로드 객체를 검증한다")
    void confirm_authorized_verifiesUpload() {
        chatImageService.confirmUpload(PARTY_ID, USER_ID, new ChatImageConfirmRequest(CHAT_KEY));

        verify(chatAuthValidator).validateChatAccess(PARTY_ID, USER_ID);
        verify(imageStorageService).confirmUpload(CHAT_KEY);
    }

    @Test
    @DisplayName("채팅 접근 권한이 없으면 확정이 거부되고 검증을 호출하지 않는다")
    void confirm_unauthorized_throws() {
        givenNoChatAccess();

        assertThatThrownBy(() ->
                chatImageService.confirmUpload(PARTY_ID, USER_ID, new ChatImageConfirmRequest(CHAT_KEY)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(imageStorageService, never()).confirmUpload(anyString());
    }

    @Test
    @DisplayName("접근 권한이 있어도 다른 팟의 key로 확정하면 거부되고 검증을 호출하지 않는다")
    void confirm_keyFromAnotherParty_throws() {
        // 접근 권한은 통과(기본 mock은 아무것도 안 던짐), 하지만 key는 다른 팟(chat/999/)의 것 → 거부
        String otherPartyKey = "chat/999/1f0a2c4e-1b3d-4f5a-8c9d-0e1f2a3b4c5d.jpg";

        assertThatThrownBy(() ->
                chatImageService.confirmUpload(PARTY_ID, USER_ID, new ChatImageConfirmRequest(otherPartyKey)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(imageStorageService, never()).confirmUpload(anyString());
    }
}
