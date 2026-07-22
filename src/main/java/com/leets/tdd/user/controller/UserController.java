package com.leets.tdd.user.controller;

import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원 정보 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "마이페이지 조회",
            description = "닉네임/프로필사진/매너온도/노쇼 제한 상태/기숙사 인증 상태를 조회한다. "
                    + "Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        MyPageResponse response = userService.getMyPage(authorization);
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회에 성공하였습니다.", response));
    }
}
