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
    SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배달팟 검색에 실패했습니다."),
    COMPLETE_FORBIDDEN(HttpStatus.FORBIDDEN, "배달 완료를 처리할 권한이 없습니다."),
    COMPLETE_NOT_ORDERED(HttpStatus.BAD_REQUEST, "주문이 완료된 배달팟만 배달 완료 처리할 수 있습니다."),
    ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 배달이 완료된 배달팟입니다."),
    COMPLETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배달 완료 처리에 실패했습니다."),
    ORDER_FORBIDDEN(HttpStatus.FORBIDDEN, "주문 완료를 처리할 권한이 없습니다."),
    ORDER_NOT_CLOSED(HttpStatus.BAD_REQUEST, "모집이 마감된 배달팟만 주문 완료할 수 있습니다."),
    ALREADY_ORDERED(HttpStatus.BAD_REQUEST, "이미 주문이 완료된 배달팟입니다."),
    ORDER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "주문 완료 처리에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
