package com.leets.tdd.user.scheduler;

import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.global.storage.exception.ImageErrorCode;
import com.leets.tdd.global.storage.exception.ImageException;
import com.leets.tdd.user.service.DormVerificationExpiryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// DB 반영(DormVerificationExpiryService)과 S3 삭제를 분리한 뒤의 스케줄러는 순수 오케스트레이션만
// 한다 - "삭제할 key 목록을 받아서, 하나씩 지운다(하나 실패해도 나머지는 계속)"만 검증하면 된다.
// DB 쪽 로직(EXPIRED 전환, key 수집)은 DormVerificationExpiryServiceTest에서 별도로 검증한다.
@ExtendWith(MockitoExtension.class)
class DormVerificationExpirySchedulerTest {

    @Mock
    private DormVerificationExpiryService dormVerificationExpiryService;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private DormVerificationExpiryScheduler scheduler;

    @Test
    @DisplayName("삭제할 이미지가 없으면 S3를 호출하지 않는다")
    void expireOverdueVerifications_noImageKeys_doesNotCallDelete() {
        when(dormVerificationExpiryService.expireOverdueVerifications()).thenReturn(List.of());

        scheduler.expireOverdueVerifications();

        verify(imageStorageService, never()).delete(any());
    }

    @Test
    @DisplayName("삭제할 이미지가 있으면 각각 S3에서 지운다")
    void expireOverdueVerifications_imageKeysReturned_deletesEach() {
        when(dormVerificationExpiryService.expireOverdueVerifications())
                .thenReturn(List.of("dormitory-verifications/1/a.jpg", "dormitory-verifications/2/b.jpg"));

        scheduler.expireOverdueVerifications();

        verify(imageStorageService).delete("dormitory-verifications/1/a.jpg");
        verify(imageStorageService).delete("dormitory-verifications/2/b.jpg");
    }

    @Test
    @DisplayName("특정 이미지 삭제가 실패해도 나머지 이미지는 계속 삭제를 시도한다")
    void expireOverdueVerifications_oneDeletionFails_continuesWithRest() {
        when(dormVerificationExpiryService.expireOverdueVerifications())
                .thenReturn(List.of("dormitory-verifications/1/a.jpg", "dormitory-verifications/2/b.jpg"));
        doThrow(new ImageException(ImageErrorCode.IMAGE_STORAGE_ERROR))
                .when(imageStorageService).delete("dormitory-verifications/1/a.jpg");

        scheduler.expireOverdueVerifications();

        verify(imageStorageService).delete("dormitory-verifications/1/a.jpg");
        verify(imageStorageService).delete("dormitory-verifications/2/b.jpg");
    }
}
