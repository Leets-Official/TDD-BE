package com.leets.tdd.global.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * S3 공개(public) 버킷을 다루는 공용 인프라(프로필 이미지 전용). 채팅·기숙사 인증 이미지는
 * 비공개 버킷(ImageStorageService)을 쓰므로 이 클래스 범위 밖이다.
 * <p>
 * 업로드 흐름은 ImageStorageService와 동일한 presign/upload/confirm 3단계다. 차이는 조회 방식뿐이다 -
 * 공개 버킷이라 Presigned GET을 발급하지 않고, base URL(S3_PUBLIC_BASE_URL) + key로 완성된
 * 공개 URL을 바로 조립한다(만료가 없어 DB/응답에 그대로 써도 된다).
 * <p>
 * key는 항상 서버가 인증된 사용자의 userId로 만든다(profiles/{userId}/{uuid}.{ext}). confirm
 * 시점에는 호출자가 본인 몫의 key를 보냈는지 다시 검증한다(belongsTo) - key의 id 자체를 권한
 * 근거로 삼지 않기 위함이다.
 */
@Slf4j
@Component
public class ProfileImageStorageService {

    private static final String KEY_PREFIX = "profiles";
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB
    private static final Duration PRESIGNED_PUT_EXPIRATION = Duration.ofMinutes(5);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String publicBucket;
    private final String publicBaseUrl;

    public ProfileImageStorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.public-bucket}") String publicBucket,
            @Value("${aws.s3.public-base-url}") String publicBaseUrl
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.publicBucket = publicBucket;
        // 끝에 슬래시가 붙어 있어도/없어도 동작하도록 여기서 한 번 정리해둔다.
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    /** 프로필 이미지용 key(profiles/{userId}/{uuid}.{ext})를 새로 만든다. */
    public String buildProfileImageKey(Long userId, String contentType) {
        ImageContentType type = resolveImageContentType(contentType);
        return "%s/%d/%s.%s".formatted(KEY_PREFIX, userId, UUID.randomUUID(), type.extension());
    }

    /** contentType이 허용 목록(JPEG/PNG/WEBP)에 있는지 검증하고 매핑된 타입을 반환한다. */
    public ImageContentType resolveImageContentType(String contentType) {
        return ImageContentType.from(contentType)
                .orElseThrow(() -> new InvalidImageException("JPEG, PNG, WEBP 형식의 이미지만 업로드할 수 있습니다."));
    }

    /** key가 profiles/userId/ 아래에 있는(=본인 몫의) 단일 세그먼트 key인지 검증한다. */
    public boolean belongsTo(Long userId, String key) {
        if (key == null) {
            return false;
        }
        String expectedPrefix = "%s/%d/".formatted(KEY_PREFIX, userId);
        if (!key.startsWith(expectedPrefix)) {
            return false;
        }
        String remainder = key.substring(expectedPrefix.length());
        return !remainder.isEmpty() && !remainder.contains("/") && !remainder.contains("..");
    }

    /** 지정된 key로 Presigned PUT URL을 발급한다. 실제 PUT 요청은 여기서 서명한 contentType과 일치해야 한다. */
    public String generatePresignedPutUrl(String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(publicBucket)
                .key(key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_PUT_EXPIRATION)
                .putObjectRequest(putObjectRequest)
                .build();
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    /**
     * key로 실제 S3에 올라간 객체의 메타데이터(용량/타입)를 조회한다. 아직 업로드되지 않았거나
     * 잘못된 key면(존재하지 않으면) 빈 값을 반환한다.
     */
    public Optional<UploadedObjectMeta> headObject(String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(publicBucket).key(key).build());
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
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(publicBucket).key(key).build());
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

    /** 공개 버킷은 만료 없는 완성된 URL을 바로 조립해서 돌려준다(Presigned GET을 쓰지 않음). */
    public String buildPublicUrl(String key) {
        if (key == null) {
            return null;
        }
        return publicBaseUrl + "/" + key;
    }
}
