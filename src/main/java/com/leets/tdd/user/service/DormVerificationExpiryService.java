package com.leets.tdd.user.service;

import com.leets.tdd.user.domain.DormStatus;
import com.leets.tdd.user.domain.Dormitory;
import com.leets.tdd.user.repository.DormitoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 기숙사 인증 만료 배치의 DB 갱신 전용 로직(FR-AUTH-02). S3 이미지 삭제(외부 I/O)는 의도적으로
 * 이 트랜잭션 밖에서 처리한다 - 같은 트랜잭션 안에서 S3 삭제까지 같이 하면, 배치 중간에 삭제가
 * 실패해 트랜잭션이 롤백될 때 이미 지워버린 S3 객체는 되돌릴 수 없어서 DB(여전히 APPROVED)와
 * S3(이미 삭제됨)가 서로 어긋나는 상태가 생길 수 있다(CodeRabbit 리뷰 지적).
 * <p>
 * 그래서 "DB에서 EXPIRED로 확정 짓는 것"을 먼저 별도 트랜잭션으로 커밋하고, 그 뒤에 지울
 * 이미지 key 목록만 호출자(DormVerificationExpiryScheduler)에게 돌려준다 - S3 삭제는 호출자가
 * 이 트랜잭션이 끝난 뒤 별도로, 건별로 실패를 격리해서 처리한다.
 */
@Service
@RequiredArgsConstructor
public class DormVerificationExpiryService {

    private final DormitoryRepository dormitoryRepository;

    @Transactional
    public List<String> expireOverdueVerifications() {
        List<Dormitory> overdue = dormitoryRepository
                .findByDormStatusAndDormVerifiedUntilBefore(DormStatus.APPROVED, LocalDateTime.now());
        if (overdue.isEmpty()) {
            return List.of();
        }

        List<String> imageKeysToDelete = new ArrayList<>();
        for (Dormitory dormitory : overdue) {
            String imageKey = dormitory.getDormVerificationImageKey();
            dormitory.expire();
            if (imageKey != null) {
                imageKeysToDelete.add(imageKey);
            }
        }
        return imageKeysToDelete;
    }
}
