package com.leets.tdd.user.service;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DormVerificationExpiryServiceTest {

    @Mock
    private DormitoryRepository dormitoryRepository;

    @InjectMocks
    private DormVerificationExpiryService dormVerificationExpiryService;

    @Test
    @DisplayName("만료 대상이 없으면 빈 목록을 반환한다")
    void expireOverdueVerifications_noOverdue_returnsEmptyList() {
        when(dormitoryRepository.findByDormStatusAndDormVerifiedUntilBefore(eq(DormStatus.APPROVED), any()))
                .thenReturn(List.of());

        List<String> imageKeys = dormVerificationExpiryService.expireOverdueVerifications();

        assertThat(imageKeys).isEmpty();
    }

    @Test
    @DisplayName("만료 대상이 있으면 EXPIRED로 전환하고 삭제할 이미지 key를 반환한다")
    void expireOverdueVerifications_overdueFound_expiresAndReturnsImageKey() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", "dormitory-verifications/1/uuid.jpg");
        dormitory.approve(LocalDateTime.now().minusDays(1));

        when(dormitoryRepository.findByDormStatusAndDormVerifiedUntilBefore(eq(DormStatus.APPROVED), any()))
                .thenReturn(List.of(dormitory));

        List<String> imageKeys = dormVerificationExpiryService.expireOverdueVerifications();

        assertThat(dormitory.getDormStatus()).isEqualTo(DormStatus.EXPIRED);
        assertThat(imageKeys).containsExactly("dormitory-verifications/1/uuid.jpg");
    }

    @Test
    @DisplayName("여러 건이 만료 대상이면 전부 EXPIRED로 전환하고 각각의 key를 반환한다")
    void expireOverdueVerifications_multipleOverdue_expiresAllAndReturnsAllKeys() {
        Dormitory first = new Dormitory(1L, "1기숙사", "dormitory-verifications/1/a.jpg");
        first.approve(LocalDateTime.now().minusDays(2));
        Dormitory second = new Dormitory(2L, "2기숙사", "dormitory-verifications/2/b.jpg");
        second.approve(LocalDateTime.now().minusHours(1));

        when(dormitoryRepository.findByDormStatusAndDormVerifiedUntilBefore(eq(DormStatus.APPROVED), any()))
                .thenReturn(List.of(first, second));

        List<String> imageKeys = dormVerificationExpiryService.expireOverdueVerifications();

        assertThat(first.getDormStatus()).isEqualTo(DormStatus.EXPIRED);
        assertThat(second.getDormStatus()).isEqualTo(DormStatus.EXPIRED);
        assertThat(imageKeys).containsExactlyInAnyOrder(
                "dormitory-verifications/1/a.jpg", "dormitory-verifications/2/b.jpg");
    }

    @Test
    @DisplayName("이미지 key가 없는 건은 상태만 전환하고 반환 목록에는 포함하지 않는다")
    void expireOverdueVerifications_withoutImageKey_excludesFromReturnedKeys() {
        Dormitory dormitory = new Dormitory(1L, "1기숙사", null);
        dormitory.approve(LocalDateTime.now().minusDays(1));

        when(dormitoryRepository.findByDormStatusAndDormVerifiedUntilBefore(eq(DormStatus.APPROVED), any()))
                .thenReturn(List.of(dormitory));

        List<String> imageKeys = dormVerificationExpiryService.expireOverdueVerifications();

        assertThat(dormitory.getDormStatus()).isEqualTo(DormStatus.EXPIRED);
        assertThat(imageKeys).isEmpty();
    }
}
