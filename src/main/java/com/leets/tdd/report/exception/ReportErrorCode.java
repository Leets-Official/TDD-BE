package com.leets.tdd.report.exception;

import com.leets.tdd.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {
  PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "배달팟을 찾을 수 없습니다."),
  NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "배달팟 참여자만 신고할 수 있습니다."),
  REPORTED_USER_NOT_PARTICIPANT(HttpStatus.BAD_REQUEST, "신고 대상이 배달팟 참여자가 아닙니다."),
  SELF_REPORT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신을 신고할 수 없습니다."),
  REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "동일한 신고가 접수되어 처리 중입니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

  private final HttpStatus httpStatus;
  private final String message;
}
