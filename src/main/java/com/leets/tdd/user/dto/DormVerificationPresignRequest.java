package com.leets.tdd.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 기숙사 인증 이미지 업로드 1단계(발급) 요청. 브라우저가 S3에 직접 올릴 파일의 MIME 타입을
 * 미리 알려주면, 서버가 이 타입으로 서명된 Presigned PUT URL을 만들어 돌려준다.
 */
public record DormVerificationPresignRequest(

        @NotBlank(message = "이미지 형식(contentType)을 입력해주세요.")
        @Pattern(regexp = "^(image/jpeg|image/png|image/webp)$",
                message = "JPEG, PNG, WEBP 형식만 업로드할 수 있습니다.")
        String contentType
) {
}
