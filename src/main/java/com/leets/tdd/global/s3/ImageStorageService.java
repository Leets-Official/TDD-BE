package com.leets.tdd.global.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * S3 비공개(private) 버킷을 다루는 공용 인프라(채팅·기숙사 인증용). 프로필 이미지는 별도의
 * 공개(public) 버킷 + 고정 base URL 조합으로 처리되므로 이 클래스 범위 밖이다.
 * <p>
 * 업로드는 "서버가 파일을 대신 받아 PutObject" 하지 않는다 - 브라우저가 S3에 직접 PUT하고,
 * 서버는 발급(presign)과 확정(confirm) 두 단계만 담당하는 3단계 흐름이다.
 * <pre>
 * 1. presign: 서버가 권한/확장자를 검증하고 key + Presigned PUT URL을 발급한다(DB는 안 건드림).
 * 2. upload : 브라우저가 그 URL로 S3에 직접 PUT한다(서버를 거치지 않음).
 * 3. confirm: 서버가 HeadObject로 실제 업로드된 객체의 용량/타입을 확인한다.
 *             기준을 벗어나면 DeleteObject 후 실패 처리하고, 통과해야만 호출자가 DB에 key를 저장한다.
 * </pre>
 * presigned PUT만으로는 클라이언트가 실제로 무엇을 얼마나 올렸는지 서버가 확인할 수 없기 때문에
 * (프론트 검증은 신뢰 경계가 아님), 이 confirm 단계의 HeadObject가 유일한 신뢰 가능한 검증이다.
 * <p>
 * key는 항상 서버가 인증된 사용자의 userId로 직접 만든다({prefix}/{userId}/{uuid}.{ext}). presign이
 * 발급한 key라도, confirm 시점에는 호출자가 "지금 로그인한 사용자 본인 몫의 key"인지 다시 검증해야
 * 한다(belongsTo) - key에 적힌 id 자체를 권한 근거로 삼지 않기 위함이다.
 */
@Slf4j
@Component
public class ImageStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB
    private static final Duration PRESIGNED_PUT_EXPIRATION = Duration.ofMinutes(5);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String privateBucket;
    private final Duration presignedGetExpiration;

    public ImageStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.private-bucket}") String privateBucket,
            @Value("${aws.s3.presigned-get-expiration-seconds}") long presignedGetExpirationSeconds
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.privateBucket = privateBucket;
        this.presignedGetExpiration = Duration.ofSeconds(presignedGetExpirationSeconds);
    }

    /**
     * 기숙사 인증 이미지용 key(dormitory-verifications/{userId}/{uuid}.{ext})를 새로 만든다.
     * contentType이 허용 목록(JPEG/PNG/WEBP)에 없으면 예외를 던진다.
     */
    public String buildDormVerificationKey(Long userId, String contentType) {
        ImageContentType type = resolveImageContentType(contentType);
        return "dormitory-verifications/%d/%s.%s".formatted(userId, UUID.randomUUID(), type.extension());
    }

    /** contentType이 허용 목록에 있는지 검증하고 매핑된 타입을 반환한다. */
    public ImageContentType resolveImageContentType(String contentType) {
        return ImageContentType.from(contentType)
                .orElseThrow(() -> new InvalidImageException("JPEG, PNG, WEBP 형식의 이미지만 업로드할 수 있습니다."));
    }

    /**
     * key가 keyPrefix/userId/ 아래에 있는(=본인 몫의) 단일 세그먼트 key인지 검증한다.
     * confirm 단계에서 클라이언트가 보낸 key를 그대로 신뢰하지 않기 위한 방어용 체크다.
     */
    public boolean belongsTo(String keyPrefix, Long userId, String key) {
        if (key == null) {
            return false;
        }
        String expectedPrefix = "%s/%d/".formatted(keyPrefix, userId);
        if (!key.startsWith(expectedPrefix)) {
            return false;
        }
        String remainder = key.substring(expectedPrefix.length());
        return !remainder.isEmpty() && !remainder.contains("/") && !remainder.contains("..");
    }

    /** 지정된 key로 Presigned PUT URL을 발급한다. 실제 PUT 요청은 여기서 서명한 contentType과 일치해야 한다. */
    public String generatePresignedPutUrl(String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(privateBucket)
                .key(key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_PUT_EXPIRATION)
                .putObjectRequest(putObjectRequest)
                .build();
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    /** 저장된 key로 짧은 만료 시간의 Presigned GET URL을 발급한다. */
    public String generatePresignedGetUrl(String key) {
        if (key == null) {
            return null;
        }
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(privateBucket)
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignedGetExpiration)
                .getObjectRequest(getObjectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * key로 실제 S3에 올라간 객체의 메타데이터(용량/타입)를 조회한다. 아직 업로드되지 않았거나
     * 잘못된 key면(존재하지 않으면) 빈 값을 반환한다 - 이 경우가 "발급만 받고 안 올린 고아 요청"이다.
     */
    public Optional<UploadedObjectMeta> headObject(String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(privateBucket).key(key).build());
            return Optional.of(new UploadedObjectMeta(response.contentLength(), response.contentType()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /** 용량/타입 기준을 벗어나 확정에 실패한 객체를 지운다(버킷에 고아 객체로 남지 않도록). */
    public void deleteObject(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(privateBucket).key(key).build());
        } catch (S3Exception e) {
            log.warn("S3 객체 삭제 실패(무시하고 진행): key={}", key, e);
        }
    }

    public boolean isSizeWithinLimit(long contentLength) {
        return contentLength <= MAX_FILE_SIZE_BYTES;
    }

    public boolean isAllowedContentType(String contentType) {
        return ImageContentType.from(contentType).isPresent();
    }

    /** HeadObject 응답에서 필요한 필드만 추린 값. */
    public record UploadedObjectMeta(long contentLength, String contentType) {
    }
}
