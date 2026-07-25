package com.leets.tdd.global.storage;

import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
import com.leets.tdd.global.storage.exception.ImageErrorCode;
import com.leets.tdd.global.storage.exception.ImageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public PresignedUploadResponse issueUploadUrl(ImageCategory category, long ownerId, String contentType) {
        ImageContentType imageContentType = ImageContentType.from(contentType);
        String key = buildKey(category, ownerId, imageContentType);
        Duration validity = Duration.ofSeconds(s3Properties.presignedUrlValiditySeconds());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(resolveBucket(category))
                .key(key)
                .contentType(imageContentType.getMimeType())
                .build();

        String uploadUrl = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(validity)
                        .putObjectRequest(putObjectRequest)
                        .build())
                .url()
                .toString();

        return new PresignedUploadResponse(key, uploadUrl, imageContentType.getMimeType(), validity.toSeconds());
    }

    @Override
    public void confirmUpload(String key) {
        ImageCategory category = ImageCategory.fromKey(key);
        String bucket = resolveBucket(category);
        HeadObjectResponse uploaded = headObject(bucket, key);

        Long contentLength = uploaded.contentLength();
        if (contentLength == null || contentLength > s3Properties.maxFileSizeBytes()) {
            deleteObject(bucket, key);
            throw new ImageException(ImageErrorCode.IMAGE_TOO_LARGE);
        }

        if (ImageContentType.find(uploaded.contentType()).isEmpty()) {
            deleteObject(bucket, key);
            throw new ImageException(ImageErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
    }

    @Override
    public String resolveViewUrl(String key) {
        ImageCategory category = ImageCategory.fromKey(key);
        if (category.isPubliclyReadable()) {
            return buildPublicUrl(key);
        }
        return presignGetUrl(resolveBucket(category), key);
    }

    @Override
    public void delete(String key) {
        ImageCategory category = ImageCategory.fromKey(key);
        deleteObject(resolveBucket(category), key);
    }

    private String buildKey(ImageCategory category, long ownerId, ImageContentType contentType) {
        return "%s/%d/%s.%s".formatted(
                category.getPrefix(),
                ownerId,
                UUID.randomUUID(),
                contentType.getExtension());
    }

    private String resolveBucket(ImageCategory category) {
        return category.isPubliclyReadable() ? s3Properties.publicBucket() : s3Properties.privateBucket();
    }

    private String buildPublicUrl(String key) {
        String baseUrl = s3Properties.publicBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + key;
    }

    private String presignGetUrl(String bucket, String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(s3Properties.presignedUrlValiditySeconds()))
                        .getObjectRequest(getObjectRequest)
                        .build())
                .url()
                .toString();
    }

    private HeadObjectResponse headObject(String bucket, String key) {
        try {
            return s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ImageException(ImageErrorCode.IMAGE_NOT_UPLOADED);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ImageException(ImageErrorCode.IMAGE_NOT_UPLOADED);
            }
            log.error("S3 headObject 실패. bucket={}, key={}", bucket, key, e);
            throw new ImageException(ImageErrorCode.IMAGE_STORAGE_ERROR);
        }
    }

    private void deleteObject(String bucket, String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            log.error("S3 deleteObject 실패. bucket={}, key={}", bucket, key, e);
            throw new ImageException(ImageErrorCode.IMAGE_STORAGE_ERROR);
        }
    }
}
