package com.leets.tdd.chat.controller;

import com.leets.tdd.chat.dto.ChatImageConfirmRequest;
import com.leets.tdd.chat.dto.ChatImagePresignRequest;
import com.leets.tdd.chat.dto.ChatImagePresignResponse;
import com.leets.tdd.chat.service.ChatImageService;
import com.leets.tdd.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parties/{partyId}/chat/images")
@RequiredArgsConstructor
public class ChatImageController {

    private final ChatImageService chatImageService;

    @Operation(
            summary = "채팅 이미지 업로드 1단계(업로드 URL 발급)",
            description = "채팅 이미지를 올릴 Presigned PUT URL과 key를 발급한다(비공개 버킷). 응답으로 받은 "
                    + "uploadUrl로 브라우저가 S3에 직접 PUT한 뒤, 그 key로 확정(confirm) API를 호출해야 한다. "
                    + "허용 형식은 JPEG/PNG/WEBP다. Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/presign")
    public ResponseEntity<ApiResponse<ChatImagePresignResponse>> presignUpload(
            @PathVariable Long partyId,
            @Valid @RequestBody ChatImagePresignRequest request
    ) {
        ChatImagePresignResponse response = chatImageService.presignUpload(partyId, request);
        return ResponseEntity.ok(ApiResponse.success("업로드 URL이 발급되었습니다.", response));
    }

    @Operation(
            summary = "채팅 이미지 업로드 2단계(업로드 확정)",
            description = "브라우저가 S3에 직접 업로드를 마친 뒤 호출한다. 서버가 실제로 올라간 객체의 "
                    + "용량/형식을 확인하고, 기준을 벗어나면 객체를 지우고 실패 처리한다. "
                    + "확정에 성공하면 클라이언트는 받은 key로 { messageType: \"IMAGE\", imageUrl: key } 형태의 "
                    + "메시지를 WebSocket(/app/parties/{partyId}/chat)으로 발행해 이미지 메시지를 전송한다. "
                    + "Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmUpload(
            @PathVariable Long partyId,
            @Valid @RequestBody ChatImageConfirmRequest request
    ) {
        chatImageService.confirmUpload(partyId, request);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드가 확정되었습니다.", null));
    }
}
