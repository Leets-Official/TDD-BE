package com.leets.tdd.user.scheduler;

import com.leets.tdd.global.storage.ImageStorageService;
import com.leets.tdd.user.service.DormVerificationExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 기숙사 인증 만료 배치(FR-AUTH-02). 기숙사 인증은 학기 단위로 유지되므로, 매일 00:00에
 * dormVerifiedUntil이 지난 승인(APPROVED) 건을 찾아 EXPIRED로 전환하고, 더 이상 유효하지 않은
 * 인증 사진을 S3에서 함께 삭제한다(만료된 학기 인증서를 계속 보관할 이유가 없다).
 * <p>
 * DB 반영({@link DormVerificationExpiryService}, 별도 트랜잭션)과 S3 삭제(이 클래스, 트랜잭션
 * 밖)를 의도적으로 분리했다 - 같은 트랜잭션 안에서 S3까지 지우면 트랜잭션이 롤백될 때 이미 지운
 * S3 객체를 되돌릴 수 없어 DB/S3 상태가 어긋날 수 있다(CodeRabbit 리뷰 반영). DB 커밋이 먼저
 * 끝난 뒤에만 S3 삭제를 시도하고, 이미지 하나의 삭제가 실패해도(네트워크 문제 등) 나머지 이미지
 * 삭제와 이미 커밋된 EXPIRED 상태에는 영향이 없다 - 실패는 로그로 남기고 이번 배치에서 자동
 * 재시도하지는 않으므로, 반복적으로 실패하는 key가 있으면 운영 로그로 확인해 수동 정리한다.
 * <p>
 * REJECTED 건은 이 배치 대상이 아니다 - 재제출을 유도하기 위해 즉시 삭제하지 않는 정책이 이미
 * confirm/reject 경로에 반영되어 있고, 이 배치는 "승인됐다가 학기가 끝나서 만료되는" 경우만 다룬다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DormVerificationExpiryScheduler {

    private final DormVerificationExpiryService dormVerificationExpiryService;
    private final ImageStorageService imageStorageService;

    @Scheduled(cron = "0 0 0 * * *")
    public void expireOverdueVerifications() {
        List<String> imageKeysToDelete = dormVerificationExpiryService.expireOverdueVerifications();
        if (imageKeysToDelete.isEmpty()) {
            return;
        }

        int deleted = 0;
        for (String imageKey : imageKeysToDelete) {
            try {
                imageStorageService.delete(imageKey);
                deleted++;
            } catch (RuntimeException e) {
                log.warn("만료된 기숙사 인증 이미지 삭제 실패(DB는 이미 EXPIRED로 반영됨): key={}", imageKey, e);
            }
        }

        log.info("기숙사 인증 만료 배치 완료: {}건 EXPIRED 처리, 이미지 {}건 삭제", imageKeysToDelete.size(), deleted);
    }
}
