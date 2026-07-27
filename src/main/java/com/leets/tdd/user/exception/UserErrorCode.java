package com.leets.tdd.user.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode {

    NICKNAME_DUPLICATE(HttpStatus.BAD_REQUEST, "닉네임이 중복입니다."),
    INVALID_VERIFICATION(HttpStatus.BAD_REQUEST, "인증정보가 유효하지 않습니다. 이메일 인증을 다시 해주세요."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "인증 토큰이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ACCOUNT_BANNED(HttpStatus.FORBIDDEN, "이용이 제한된 계정입니다."),
    WITHDRAWAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "이용 제한 중에는 탈퇴할 수 없습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    NEW_PASSWORD_SAME_AS_CURRENT(HttpStatus.BAD_REQUEST, "새 비밀번호가 기존 비밀번호와 동일합니다."),
    REGISTRATION_BLOCKED(HttpStatus.BAD_REQUEST, "가입할 수 없는 이메일입니다."),
    ALREADY_REGISTERED_EMAIL(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다."),
    NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "닉네임 자동 생성에 실패했습니다. 잠시 후 다시 시도해주세요."),
    ACTIVE_POT_EXISTS(HttpStatus.BAD_REQUEST, "진행 중인 배달팟이 있어 탈퇴할 수 없습니다."),
    UNSETTLED_POT_EXISTS(HttpStatus.BAD_REQUEST, "정산 중인 배달팟이 있어 탈퇴할 수 없습니다."),
    DORM_VERIFICATION_ALREADY_IN_PROGRESS(HttpStatus.BAD_REQUEST, "이미 인증 신청이 진행 중이거나 승인된 상태입니다."),
    INVALID_DORM_VERIFICATION_IMAGE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    DORM_VERIFICATION_UPLOAD_NOT_FOUND(HttpStatus.BAD_REQUEST, "업로드된 이미지를 찾을 수 없습니다. 다시 시도해주세요."),
    INVALID_PROFILE_IMAGE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    PROFILE_IMAGE_UPLOAD_NOT_FOUND(HttpStatus.BAD_REQUEST, "업로드된 이미지를 찾을 수 없습니다. 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}
