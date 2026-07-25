package com.leets.tdd.global.storage;

import com.leets.tdd.global.storage.dto.PresignedUploadResponse;
import com.leets.tdd.global.storage.exception.ImageErrorCode;
import com.leets.tdd.global.storage.exception.ImageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("S3ImageStorageService")
class S3ImageStorageServiceTest {

    private static final String PRIVATE_BUCKET = "tdd-assets-test";
    private static final String PUBLIC_BUCKET = "tdd-public-test";
    private static final String PUBLIC_BASE_URL = "https://tdd-public-test.s3.ap-northeast-2.amazonaws.com";
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3ImageStorageService imageStorageService;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties(
                PRIVATE_BUCKET,
                PUBLIC_BUCKET,
                PUBLIC_BASE_URL,
                "ap-northeast-2",
                300L,
                MAX_FILE_SIZE_BYTES);
        imageStorageService = new S3ImageStorageService(s3Client, s3Presigner, properties);
    }

    @Nested
    @DisplayName("업로드 URL 발급")
    class IssueUploadUrl {

        @Test
        @DisplayName("카테고리별 접두사와 소유자 ID로 key를 만든다")
        void buildsKeyWithCategoryPrefixAndOwnerId() {
            givenPresignedPutUrl();

            PresignedUploadResponse profile = imageStorageService.issueUploadUrl(ImageCategory.PROFILE, 7L, "image/jpeg");
            PresignedUploadResponse chat = imageStorageService.issueUploadUrl(ImageCategory.CHAT, 42L, "image/png");
            PresignedUploadResponse dormitory =
                    imageStorageService.issueUploadUrl(ImageCategory.DORMITORY_VERIFICATION, 7L, "image/webp");

            assertThat(profile.key()).startsWith("profiles/7/").endsWith(".jpg");
            assertThat(chat.key()).startsWith("chat/42/").endsWith(".png");
            assertThat(dormitory.key()).startsWith("dormitory-verifications/7/").endsWith(".webp");
        }

        @Test
        @DisplayName("공개 이미지는 공개 버킷에, 비공개 이미지는 비공개 버킷에 서명한다")
        void signsAgainstBucketMatchingVisibility() {
            givenPresignedPutUrl();
            ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);

            imageStorageService.issueUploadUrl(ImageCategory.PROFILE, 1L, "image/jpeg");
            imageStorageService.issueUploadUrl(ImageCategory.CHAT, 1L, "image/jpeg");

            verify(s3Presigner, times(2)).presignPutObject(captor.capture());
            assertThat(captor.getAllValues().get(0).putObjectRequest().bucket()).isEqualTo(PUBLIC_BUCKET);
            assertThat(captor.getAllValues().get(1).putObjectRequest().bucket()).isEqualTo(PRIVATE_BUCKET);
        }

        @Test
        @DisplayName("Content-Type을 서명에 포함해 업로드 시 위조를 막는다")
        void signsContentType() {
            givenPresignedPutUrl();
            ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);

            PresignedUploadResponse response = imageStorageService.issueUploadUrl(ImageCategory.PROFILE, 1L, "image/png");

            verify(s3Presigner).presignPutObject(captor.capture());
            assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/png");
            assertThat(response.contentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("허용하지 않는 형식이면 URL을 발급하지 않는다")
        void rejectsUnsupportedContentType() {
            assertThatThrownBy(() -> imageStorageService.issueUploadUrl(ImageCategory.PROFILE, 1L, "application/pdf"))
                    .isInstanceOf(ImageException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.UNSUPPORTED_CONTENT_TYPE);

            verify(s3Presigner, never()).presignPutObject(any(PutObjectPresignRequest.class));
        }
    }

    @Nested
    @DisplayName("업로드 확정")
    class ConfirmUpload {

        @Test
        @DisplayName("제한 이내의 이미지는 통과시킨다")
        void acceptsImageWithinLimit() {
            givenUploadedObject(MAX_FILE_SIZE_BYTES, "image/jpeg");

            imageStorageService.confirmUpload("profiles/7/abc.jpg");

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("크기 제한을 넘으면 객체를 삭제하고 거부한다")
        void deletesAndRejectsOversizedImage() {
            givenUploadedObject(MAX_FILE_SIZE_BYTES + 1, "image/jpeg");

            assertThatThrownBy(() -> imageStorageService.confirmUpload("chat/42/abc.jpg"))
                    .isInstanceOf(ImageException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_TOO_LARGE);

            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            verify(s3Client).deleteObject(captor.capture());
            assertThat(captor.getValue().bucket()).isEqualTo(PRIVATE_BUCKET);
            assertThat(captor.getValue().key()).isEqualTo("chat/42/abc.jpg");
        }

        @Test
        @DisplayName("이미지가 아닌 파일이 올라오면 객체를 삭제하고 거부한다")
        void deletesAndRejectsNonImage() {
            givenUploadedObject(1024L, "application/pdf");

            assertThatThrownBy(() -> imageStorageService.confirmUpload("profiles/7/abc.jpg"))
                    .isInstanceOf(ImageException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.UNSUPPORTED_CONTENT_TYPE);

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("업로드되지 않은 key는 거부한다")
        void rejectsMissingObject() {
            given(s3Client.headObject(any(HeadObjectRequest.class)))
                    .willThrow(NoSuchKeyException.builder().message("not found").build());

            assertThatThrownBy(() -> imageStorageService.confirmUpload("profiles/7/abc.jpg"))
                    .isInstanceOf(ImageException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_UPLOADED);
        }
    }

    @Nested
    @DisplayName("조회 URL")
    class ResolveViewUrl {

        @Test
        @DisplayName("프로필은 만료 없는 공개 URL을 조립한다")
        void buildsPublicUrlForProfile() {
            String url = imageStorageService.resolveViewUrl("profiles/7/abc.jpg");

            assertThat(url).isEqualTo(PUBLIC_BASE_URL + "/profiles/7/abc.jpg");
            verify(s3Presigner, never()).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("채팅과 기숙사 인증은 Presigned GET URL을 발급한다")
        void presignsPrivateImages() {
            givenPresignedGetUrl();

            imageStorageService.resolveViewUrl("chat/42/abc.jpg");
            imageStorageService.resolveViewUrl("dormitory-verifications/7/abc.jpg");

            ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
            verify(s3Presigner, times(2)).presignGetObject(captor.capture());
            assertThat(captor.getAllValues())
                    .allSatisfy(request -> assertThat(request.getObjectRequest().bucket()).isEqualTo(PRIVATE_BUCKET));
        }
    }

    @Nested
    @DisplayName("key 검증")
    class KeyValidation {

        @Test
        @DisplayName("알 수 없는 접두사는 거부한다")
        void rejectsUnknownPrefix() {
            assertThatThrownBy(() -> imageStorageService.resolveViewUrl("tmp/7/abc.jpg"))
                    .isInstanceOf(ImageException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_IMAGE_KEY);
        }

        @Test
        @DisplayName("접두사만 같고 경로 구분자가 없는 key는 거부한다")
        void rejectsPrefixWithoutSeparator() {
            assertThatThrownBy(() -> imageStorageService.resolveViewUrl("profiles-backup/7/abc.jpg"))
                    .isInstanceOf(ImageException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_IMAGE_KEY);
        }

        @Test
        @DisplayName("비어 있는 key는 거부한다")
        void rejectsBlankKey() {
            assertThatThrownBy(() -> imageStorageService.resolveViewUrl(" "))
                    .isInstanceOf(ImageException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.INVALID_IMAGE_KEY);
        }
    }

    private void givenPresignedPutUrl() {
        PresignedPutObjectRequest presigned = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        given(presigned.url()).willReturn(toUrl("https://example.com/upload"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);
    }

    private void givenPresignedGetUrl() {
        PresignedGetObjectRequest presigned = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        given(presigned.url()).willReturn(toUrl("https://example.com/view"));
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presigned);
    }

    private void givenUploadedObject(long contentLength, String contentType) {
        given(s3Client.headObject(any(HeadObjectRequest.class)))
                .willReturn(HeadObjectResponse.builder()
                        .contentLength(contentLength)
                        .contentType(contentType)
                        .build());
    }

    private URL toUrl(String value) {
        try {
            return URI.create(value).toURL();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
