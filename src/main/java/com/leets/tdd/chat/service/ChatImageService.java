package com.leets.tdd.chat.service;

import com.leets.tdd.chat.dto.ChatImageConfirmRequest;
import com.leets.tdd.chat.dto.ChatImagePresignRequest;
import com.leets.tdd.chat.dto.ChatImagePresignResponse;
import com.leets.tdd.global.storage.ImageCategory;
import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatImageService {

    private final ImageStorageService imageStorageService;
    private final ChatAuthValidator chatAuthValidator;

    /**
     * 채팅 이미지 업로드 1단계(발급). 발급 전에 로그인 사용자가 해당 팟의 채팅 접근 권한
     * (팟이 종료 상태가 아니고, 방장이거나 JOINED 참여자)이 있는지 검증한다.
     * 통과하면 S3에 직접 올릴 key와 Presigned PUT URL을 발급한다(DB는 건드리지 않음).
     */
    @Transactional(readOnly = true)
    public ChatImagePresignResponse presignUpload(Long partyId, Long userId, ChatImagePresignRequest request) {
        chatAuthValidator.validateChatAccess(partyId, userId);

        PresignedUploadResponse presigned =
                imageStorageService.issueUploadUrl(ImageCategory.CHAT, partyId, request.contentType());

        return ChatImagePresignResponse.from(presigned);
    }

    /**
     * 채팅 이미지 업로드 2단계(확정). 채팅 접근 권한 + key가 해당 팟(chat/{partyId}/)의 것인지 검증한 뒤,
     * 실제로 업로드된 객체의 용량/형식이 기준 안에 있는지 검증한다(위반 시 객체 삭제 + 예외).
     * 검증만 하고 메시지는 저장하지 않는다 - 확정 통과 후 클라이언트가 그 key로 IMAGE 메시지를
     * WebSocket으로 발행하면 기존 ChatService.saveMessage가 저장·브로드캐스트한다.
     */
    @Transactional(readOnly = true)
    public void confirmUpload(Long partyId, Long userId, ChatImageConfirmRequest request) {
        chatAuthValidator.validateChatAccess(partyId, userId);
        // 요청으로 받은 key가 이 팟의 채팅 이미지 경로(chat/{partyId}/)인지 확인한다.
        // 다른 팟(예: 두 팟에 모두 참여한 사용자가 A 팟 key를 B 팟으로 확정)에서 발급된 key를
        // 넘겨 남의 이미지를 확정하는 것을 막는다.
        validateKeyBelongsToParty(partyId, request.key());

        // 용량/형식 기준을 벗어나면 confirmUpload가 ImageException을 던지면서 객체도 함께 지운다.
        imageStorageService.confirmUpload(request.key());
    }

    /**
     * 요청으로 받은 key가 해당 팟의 채팅 이미지 경로(chat/{partyId}/)로 시작하는지 확인한다.
     * 공통 모듈은 key의 접두사를 버킷 라우팅에만 쓰고 partyId 소유권은 검증하지 않으므로,
     * 도메인에서 "이 key가 이 팟의 것인지"를 확인해 교차 팟 확정을 차단한다.
     */
    private void validateKeyBelongsToParty(Long partyId, String key) {
        String expectedPrefix = ImageCategory.CHAT.getPrefix() + "/" + partyId + "/";
        if (key == null || !key.startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("해당 팟의 이미지 key가 아닙니다.");
        }
    }
}
