package com.leets.tdd.user.scheduler;

import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.user.domain.DormStatus;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.repository.DormitoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DormVerificationExpirySchedulerTest {

    @Mock
    private DormitoryRepository dormitoryRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private DormVerificationExpiryScheduler scheduler;

    @Test
    @DisplayName("만료 대상이 없으면 아무 것도 하지 않는다")
    void expireOverdueVerifications_noOverdue_doesNothing() {
        when(dormitoryRepository.findByDormStatusAndDormVerifiedUntilBefore(eq(DormStatus.APPROVED), any()))
                .thenReturn(List.of());

        scheduler.expireOverdueVerifications();

        verify(imageStorageService, never()).delete(any());
    }

    @Test
    @DisplayName("만료 대상이 있으면 EXPIRED로 전환하고 S3 이미지도 삭제한다")
    void expireOverdueVerifications_overdueFound_expiresAndDeletesImage() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "dormitory-verifications/1/uuid.jpg");
        dormitory.approve(LocalDateTime.now().minusDays(1));

        when(dormitoryRepository.findByDormStatusAndDormVerifiedUntilBefore(eq(DormStatus.APPROVED), any()))
                .thenReturn(List.of(dormitory));

        scheduler.expireOverdueVerifications();

        assertThat(dormitory.getDormStatus()).isEqualTo(DormStatus.EXPIRED);
        verify(imageStorageService).delete("dormitory-verifications/1/uuid.jpg");
    }

    @Test
    @DisplayName("여러 건이 만료 대상이면 전부 처리한다")
    void expireOverdueVerifications_multipleOverdue_expiresAll() {
        Dormitory first = new Dormitory(1L, "1기숙사", "dormitory-verifications/1/a.jpg");
        first.approve(LocalDateTime.now().minusDays(2));
        Dormitory second = new Dormitory(2L, "2기숙사", "dormitory-verifications/2/b.jpg");
        second.approve(LocalDateTime.now().minusHours(1));

        when(dormitoryRepository.findByDormStatusAndDormVerifiedUntilBefore(eq(DormStatus.APPROVED), any()))
                .thenReturn(List.of(first, second));

        scheduler.expireOverdueVerifications();

        assertThat(first.getDormStatus()).isEqualTo(DormStatus.EXPIRED);
        assertThat(second.getDormStatus()).isEqualTo(DormStatus.EXPIRED);
        verify(imageStorageService).delete("dormitory-verifications/1/a.jpg");
        verify(imageStorageService).delete("dormitory-verifications/2/b.jpg");
    }

    @Test
    @DisplayName("이미지 key가 없는 건은 삭제를 시도하지 않는다")
    void expireOverdueVerifications_withoutImageKey_doesNotCallDelete() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", null);
        dormitory.approve(LocalDateTime.now().minusDays(1));

        when(dormitoryRepository.findByDormStatusAndDormVerifiedUntilBefore(eq(DormStatus.APPROVED), any()))
                .thenReturn(List.of(dormitory));

        scheduler.expireOverdueVerifications();

        assertThat(dormitory.getDormStatus()).isEqualTo(DormStatus.EXPIRED);
        verify(imageStorageService, never()).delete(any());
    }
}
