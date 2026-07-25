package com.leets.tdd.global.s3;

import lombok.Getter;

/**
 * 업로드 이미지 검증(파일 없음/허용되지 않는 형식/용량 초과) 실패 시 던진다.
 * 이 예외는 도메인(user 등)에 특화되지 않은 공용 인프라 예외라 fieldErrors 같은 응답 형태를
 * 스스로 결정하지 않는다 - 호출한 도메인 서비스가 잡아서 자기 도메인의 예외로 옮겨 던진다
 * (예: UserService가 잡아서 InvalidDormVerificationImageException으로 변환).
 */
@Getter
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }
}
