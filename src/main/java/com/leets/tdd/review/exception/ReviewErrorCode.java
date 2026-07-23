package com.leets.tdd.review.exception;

import com.leets.tdd.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
/** 후기 API에서 반환할 HTTP 상태와 사용자 메시지를 모아 둡니다. */
public enum ReviewErrorCode implements ErrorCode {
  PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "배달팟을 찾을 수 없습니다."),
  PARTY_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 배달팟에서만 후기를 작성할 수 있습니다."),
  NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "배달팟 참여자만 이용할 수 있습니다."),
  SELF_REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신을 평가할 수 없습니다."),
  REVIEWEE_NOT_PARTICIPANT(HttpStatus.BAD_REQUEST, "평가 대상이 배달팟 참여자가 아닙니다."),
  REVIEW_TAG_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 평가 태그가 포함되어 있습니다."),
  REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 평가를 남긴 참여자입니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

  private final HttpStatus httpStatus;
  private final String message;
}
