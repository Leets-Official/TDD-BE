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
            "현재 상태에서는 배달팟을 수정할 수 없습니다."
    ),
    SEARCH_KEYWORD_REQUIRED(HttpStatus.BAD_REQUEST, "검색어를 입력해주세요."),
    SEARCH_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "검색 결과가 없습니다."),
    SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배달팟 검색에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
