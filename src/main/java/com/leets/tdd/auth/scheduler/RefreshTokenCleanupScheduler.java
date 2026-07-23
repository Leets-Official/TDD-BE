package com.leets.tdd.auth.scheduler;

import com.leets.tdd.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 만료된 refresh token 해시를 주기적으로 정리한다.
 * refresh token은 별도 테이블 없이 users.refresh_token_hash 컬럼 하나에 최신 값만 저장하는 구조라
 * "삭제"가 아니라 만료된 값을 빈 문자열로 비우는 것으로 정리한다.
 * (JWT 자체의 만료(exp) 검증만으로도 만료된 토큰은 재발급 API를 통과할 수 없어서 보안적으로
 * 반드시 필요한 작업은 아니지만, DB에 쓸모없는 해시값을 계속 남겨두지 않기 위한 위생 목적이다.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final UserRepository userRepository;

    // 매일 새벽 4시에 실행.
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        int cleared = userRepository.clearExpiredRefreshTokens(LocalDateTime.now());
        if (cleared > 0) {
            log.info("만료된 refresh token {}건 정리 완료", cleared);
        }
    }
}
