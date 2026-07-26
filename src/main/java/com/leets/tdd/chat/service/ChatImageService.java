package com.leets.tdd.chat.service;

import com.leets.tdd.chat.dto.ChatImageConfirmRequest;
import com.leets.tdd.chat.dto.ChatImagePresignRequest;
import com.leets.tdd.chat.dto.ChatImagePresignResponse;
import com.leets.tdd.global.storage.ImageCategory;
import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
import com.leets.tdd.party.domain.PartyParticipantStatus;
import com.leets.tdd.party.repository.PartyParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatImageService {

    private final ImageStorageService imageStorageService;
    private final PartyParticipantRepository partyParticipantRepository;

    /**
     * 채팅 이미지 업로드 1단계(발급). 발급 전에 로그인 사용자가 해당 팟(partyId)의 참여자(JOINED)인지
     * 검증한다 - key에 적힌 partyId를 권한 근거로 삼지 않기 위해, 요청 사용자가 실제 참여자인지
     * DB로 확인한다. 통과하면 S3에 직접 올릴 key와 Presigned PUT URL을 발급한다(DB는 건드리지 않음).
     */
    @Transactional(readOnly = true)
    public ChatImagePresignResponse presignUpload(Long partyId, Long userId, ChatImagePresignRequest request) {
        validateParticipant(partyId, userId);

        PresignedUploadResponse presigned =
                imageStorageService.issueUploadUrl(ImageCategory.CHAT, partyId, request.contentType());

        return ChatImagePresignResponse.from(presigned);
    }

    /**
     * 채팅 이미지 업로드 2단계(확정). 참여자 검증 후, 실제로 업로드된 객체의 용량/형식이 기준 안에
     * 있는지 검증한다(위반 시 객체 삭제 + 예외). 검증만 하고 메시지는 저장하지 않는다 - 확정 통과 후
     * 클라이언트가 그 key로 IMAGE 메시지를 WebSocket으로 발행하면 기존 ChatService.saveMessage가
     * 저장·브로드캐스트한다.
     */
    @Transactional(readOnly = true)
    public void confirmUpload(Long partyId, Long userId, ChatImageConfirmRequest request) {
        validateParticipant(partyId, userId);

        // 용량/형식 기준을 벗어나면 confirmUpload가 ImageException을 던지면서 객체도 함께 지운다.
        imageStorageService.confirmUpload(request.key());
    }

    /**
     * 로그인 사용자가 해당 팟에 JOINED 상태로 참여 중인지 확인한다. 아니면 접근을 거부한다.
     * (배달팟 참여(join) API가 아직 없어 실제 참여자 데이터로의 통과 검증은 join API 머지 후 가능하지만,
     *  검증 로직 자체는 여기서 수행한다.)
     */
    private void validateParticipant(Long partyId, Long userId) {
        boolean isParticipant = partyParticipantRepository
                .existsByPartyIdAndUserIdAndStatus(partyId, userId, PartyParticipantStatus.JOINED);
        if (!isParticipant) {
            throw new IllegalArgumentException("해당 팟의 참여자만 채팅 이미지를 업로드할 수 있습니다.");
        }
    }
}
