package com.leets.tdd.user.controller;

import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.dto.ProfileRegistrationRequest;
import com.leets.tdd.user.dto.ProfileRegistrationResponse;
import com.leets.tdd.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            @AuthenticationPrincipal Long userId
    ) {
        MyPageResponse response = userService.getMyPage(userId);
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회에 성공하였습니다.", response));
    }

    @Operation(
            summary = "계정등록(회원가입 완료)",
            description = "이메일 인증(SIGNUP) 완료 후 15분 이내에 비밀번호/닉네임/기숙사 동을 입력해 가입을 마친다. "
                    + "signup_token 없이 email과 인증 완료 기록(DB)만으로 처리한다. "
                    + "닉네임을 생략하면 자동 배정한다. 성공 시 access/refresh 토큰을 발급한다."
    )
    @PostMapping("/me")
    public ResponseEntity<ApiResponse<ProfileRegistrationResponse>> completeSignup(
            @Valid @RequestBody ProfileRegistrationRequest request
    ) {
        ProfileRegistrationResponse response = userService.completeSignup(request);
        return ResponseEntity.ok(ApiResponse.success("프로필 등록에 성공하였습니다.", response));
    }
}
