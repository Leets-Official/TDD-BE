package com.leets.tdd.chat.controller;

import com.leets.tdd.chat.dto.ChatMessageResponse;
import com.leets.tdd.chat.dto.ChatRoomResponse;
import com.leets.tdd.chat.service.ChatService;
import com.leets.tdd.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Chat", description = "채팅 조회 관련 API")
@RestController
@RequestMapping("/api/v1/parties/{partyId}/chat")
@RequiredArgsConstructor
public class ChatQueryController {

    private final ChatService chatService;

    @Operation(
            summary = "채팅방 정보 조회",
            description = "채팅방의 최신 메시지를 조회한다. size를 지정하지 않으면 10개를 반환하며, 1~100 범위로 제한된다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoom(
            @PathVariable Long partyId
    ) {
        ChatRoomResponse response = chatService.getChatRoom(partyId);
        return ResponseEntity.ok(ApiResponse.success("채팅방 조회에 성공하였습니다.", response));
    }

    @Operation(
            summary = "메시지 내역 조회",
            description = "채팅방의 최신 메시지를 조회한다. size를 지정하지 않으면 10개를 반환한다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Long partyId,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ChatMessageResponse> responses = chatService.getRecentMessages(partyId, size);
        return ResponseEntity.ok(ApiResponse.success("메시지 내역 조회에 성공하였습니다.", responses));
    }
}
