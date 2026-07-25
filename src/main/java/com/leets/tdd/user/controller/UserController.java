package com.leets.tdd.user.controller;

import com.leets.tdd.global.jwt.UserPrincipal;
import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.user.dto.DormVerificationConfirmRequest;
import com.leets.tdd.user.dto.DormVerificationPresignRequest;
import com.leets.tdd.user.dto.DormVerificationPresignResponse;
import com.leets.tdd.user.dto.DormVerificationUploadResponse;
import com.leets.tdd.user.dto.MyPageResponse;
import com.leets.tdd.user.dto.ProfileRegistrationRequest;
import com.leets.tdd.user.dto.ProfileRegistrationResponse;
import com.leets.tdd.user.dto.ChangePasswordRequest;
import com.leets.tdd.user.dto.ProfileUpdateRequest;
import com.leets.tdd.user.dto.ProfileUpdateResponse;
import com.leets.tdd.user.dto.PushSettingRequest;
import com.leets.tdd.user.dto.PushSettingResponse;
import com.leets.tdd.user.dto.WithdrawalRequest;
import com.leets.tdd.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        MyPageResponse response = userService.getMyPage(userPrincipal.userId());
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

    @Operation(
            summary = "프로필 수정",
            description = "닉네임/기숙사 동/프로필 사진을 수정한다. 닉네임과 기숙사 동은 필수이고, "
                    + "profileImageUrl을 null로 보내면 프로필 사진을 해제한다. "
                    + "Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me/profile")
    public ResponseEntity<ApiResponse<ProfileUpdateResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        ProfileUpdateResponse response = userService.updateProfile(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("계정 수정에 성공하였습니다.", response));
    }

    @Operation(
            summary = "기숙사 인증하기 1단계(업로드 URL 발급)",
            description = "인증 사진을 올릴 Presigned PUT URL과 key를 발급한다. 응답으로 받은 uploadUrl로 "
                    + "브라우저가 S3에 직접 PUT한 뒤, 그 key로 확정(confirm) API를 호출해야 인증 신청이 "
                    + "완료된다. 허용 형식은 JPEG/PNG/WEBP이고, 이미 심사 중이거나 승인된 상태면 발급 자체가 "
                    + "거부된다. Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me/dormitory-verification/presign")
    public ResponseEntity<ApiResponse<DormVerificationPresignResponse>> presignDormVerificationUpload(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DormVerificationPresignRequest request
    ) {
        DormVerificationPresignResponse response =
                userService.presignDormVerificationUpload(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("업로드 URL이 발급되었습니다.", response));
    }

    @Operation(
            summary = "기숙사 인증하기 2단계(업로드 확정)",
            description = "브라우저가 S3에 직접 업로드를 마친 뒤 호출한다. 서버가 실제로 올라간 객체의 "
                    + "용량/형식을 확인(HeadObject)하고, 기준을 벗어나면 객체를 지우고 실패 처리한다. "
                    + "통과하면 그때 심사 대기(PENDING) 상태로 반영된다. "
                    + "Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/me/dormitory-verification/confirm")
    public ResponseEntity<ApiResponse<DormVerificationUploadResponse>> confirmDormVerificationUpload(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody DormVerificationConfirmRequest request
    ) {
        DormVerificationUploadResponse response =
                userService.confirmDormVerificationUpload(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("기숙사 인증 신청이 완료되었습니다.", response));
    }

    @Operation(
            summary = "알림 설정 변경",
            description = "전체 알림 on/off 통합 토글 하나만 바꾼다(MVP 범위, 카테고리별 세분화 없음). "
                    + "발송 방식은 클라이언트 필터링이라 서버는 User.pushEnabled 값만 갱신한다. "
                    + "Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me/push-setting")
    public ResponseEntity<ApiResponse<PushSettingResponse>> updatePushSetting(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody PushSettingRequest request
    ) {
        PushSettingResponse response = userService.updatePushSetting(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("알림 설정이 변경되었습니다.", response));
    }

    @Operation(
            summary = "비밀번호 수정",
            description = "현재 비밀번호를 재확인한 뒤 새 비밀번호로 변경한다(기존과 동일한 비밀번호는 불가). "
                    + "성공 시 기존 refresh token은 무효화되어 재로그인이 필요하다"
                    + "(access token이 살아있는 동안은 계속 사용 가능). "
                    + "Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호 수정에 성공하였습니다."));
    }

    @Operation(
            summary = "계정탈퇴",
            description = "비밀번호 재확인 후 계정을 soft delete(status=DELETED) 처리하고 refresh token을 무효화한다. "
                    + "Authorization 헤더에 access token(Bearer)이 필요하다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody WithdrawalRequest request
    ) {
        userService.withdraw(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("계정 삭제에 성공했습니다."));
    }
}
