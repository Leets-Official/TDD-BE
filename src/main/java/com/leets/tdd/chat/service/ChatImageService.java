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

    /**
     * 채팅 이미지 업로드 1단계(발급). 브라우저가 S3에 직접 올릴 key와 Presigned PUT URL을 발급한다.
     * DB는 건드리지 않는다(이미지 메시지는 이후 WebSocket 발행 시 저장됨).

     * TODO: 발급 전에 로그인 사용자가 해당 팟(partyId)의 참여자인지 검증해야 한다.
     *       배달팟 참여(join) API 구현 후 party_participants 조회로 추가 예정 (#62 선행 조건과 동일).
     */
    @Transactional(readOnly = true)
    public ChatImagePresignResponse presignUpload(Long partyId, ChatImagePresignRequest request) {
        PresignedUploadResponse presigned =
                imageStorageService.issueUploadUrl(ImageCategory.CHAT, partyId, request.contentType());

        return ChatImagePresignResponse.from(presigned);
    }

    /**
     * 채팅 이미지 업로드 2단계(확정). 브라우저가 S3에 직접 올린 뒤 호출한다.
     * 실제로 업로드된 객체의 용량/형식이 기준 안에 있는지 검증한다(위반 시 객체 삭제 + 예외).
     * 검증만 하고 메시지는 저장하지 않는다 - 확정 통과 후 클라이언트가 그 key로 IMAGE 메시지를
     * WebSocket으로 발행하면 기존 채팅 메시지 저장 경로(ChatService.saveMessage)가 처리한다.

     * TODO: confirm 시에도 로그인 사용자가 해당 팟 참여자인지 검증 필요 (join API 후속).
     */
    @Transactional(readOnly = true)
    public void confirmUpload(Long partyId, ChatImageConfirmRequest request) {
        // 용량/형식 기준을 벗어나면 confirmUpload가 ImageException을 던지면서 객체도 함께 지운다.
        imageStorageService.confirmUpload(request.key());
    }
}
