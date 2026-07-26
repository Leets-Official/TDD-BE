package com.leets.tdd.party.exception;

import com.leets.tdd.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PartyErrorCode implements ErrorCode {

    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 배달팟입니다."),
    NOT_PARTICIPANT(HttpStatus.BAD_REQUEST, "참여 중인 배달팟이 아닙니다."),
    LEAVE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "현재 상태에서는 참여를 취소할 수 없습니다."),
    LEAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배달팟 참여 취소에 실패했습니다."),
    HOST_CANNOT_LEAVE(HttpStatus.FORBIDDEN, "파티장은 참여를 취소할 수 없습니다."),
    NOT_OWNER(HttpStatus.FORBIDDEN, "배달팟을 수정할 권한이 없습니다."),
    INVALID_PARTY_STATUS(
            HttpStatus.BAD_REQUEST,
            "현재 상태에서는 배달팟을 수정할 수 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String message;
}
