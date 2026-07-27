package com.leets.tdd.party.exception;

import com.leets.tdd.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PartyErrorCode implements ErrorCode {

    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 배달팟입니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "배달팟을 수정할 권한이 없습니다."),
    INVALID_PARTY_STATUS(
            HttpStatus.BAD_REQUEST,
            "현재 상태에서는 배달팟을 변경할 수 없습니다."
    ),
    DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "배달팟을 삭제할 권한이 없습니다."),
    CANCEL_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "모집 중인 배달팟만 취소할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
