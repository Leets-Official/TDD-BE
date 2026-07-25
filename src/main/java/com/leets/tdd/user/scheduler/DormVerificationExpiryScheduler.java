package com.leets.tdd.user.scheduler;

import com.leets.tdd.global.s3.ImageStorageService;
import com.leets.tdd.user.domain.DormStatus;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.repository.DormitoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기숙사 인증 만료 배치(FR-AUTH-02). 기숙사 인증은 학기 단위로 유지되므로, 매일 00:00에
 * dormVerifiedUntil이 지난 승인(APPROVED) 건을 찾아 EXPIRED로 전환하고, 더 이상 유효하지 않은
 * 인증 사진을 S3에서 함께 삭제한다(만료된 학기 인증서를 계속 보관할 이유가 없다).
 * <p>
 * REJECTED 건은 이 배치 대상이 아니다 - 재제출을 유도하기 위해 즉시 삭제하지 않는 정책이 이미
 * confirm/reject 경로에 반영되어 있고, 이 배치는 "승인됐다가 학기가 끝나서 만료되는" 경우만 다룬다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DormVerificationExpiryScheduler {

    private final DormitoryRepository dormitoryRepository;
    private final ImageStorageService imageStorageService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireOverdueVerifications() {
        List<Dormitory> overdue = dormitoryRepository
                .findByDormStatusAndDormVerifiedUntilBefore(DormStatus.APPROVED, LocalDateTime.now());
        if (overdue.isEmpty()) {
            return;
        }

        for (Dormitory dormitory : overdue) {
            String imageKey = dormitory.getDormVerificationImageKey();
            dormitory.expire();
            if (imageKey != null) {
                imageStorageService.deleteObject(imageKey);
            }
        }

        log.info("기숙사 인증 만료 배치 완료: {}건 EXPIRED 처리", overdue.size());
    }
}
