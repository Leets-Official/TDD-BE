package com.leets.tdd.global.webpush;

import com.leets.tdd.notification.domain.Notification;
import com.leets.tdd.notification.repository.NotificationRepository;
import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.PushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 웹푸시 발송 공통 모듈.
 * 각 도메인은 자기 알림 트리거에서 이 서비스를 호출한다 (예: 댓글 등록 시 원글작성자에게).
 * "누구에게 보낼지"(userId)만 넘기면 알림 이력을 notifications에 저장하고,
 * 해당 유저의 구독 정보(User.pushEndpoint/p256dh/auth)를 꺼내 실제 브라우저 푸시 서버로 발송한다.
 *
 * 알림 이력 저장은 웹푸시 구독 여부와 무관하게 수행한다("해당 유저에게 이런 알림이 발생했다"는 기록).
 * 저장·발송 실패는 알림 특성상 치명적이지 않으므로(핵심 기능이 아님) 예외를 던지지 않고 로깅만 한다.
 * 호출한 도메인의 트랜잭션이 알림 실패로 롤백되면 안 되기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class WebPushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

    private final PushService pushService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    /**
     * 여러 사용자에게 같은 알림을 발송한다 (예: 팟 참여자 전원).
     */
    public void sendToUsers(List<Long> userIds, WebPushPayload payload) {
        for (Long userId : userIds) {
            sendToUser(userId, payload);
        }
    }

    /**
     * 한 사용자에게 알림 이력을 저장하고 웹푸시를 발송한다.
     * 이력 저장은 구독 여부와 무관하게 먼저 수행하고, 발송은 구독·알림설정이 유효한 경우에만 한다.
     */
    public void sendToUser(Long userId, WebPushPayload payload) {
        // 1) 알림 이력 저장 (웹푸시 구독 여부와 무관). 실패해도 발송·도메인에 영향 주지 않는다.
        saveNotification(userId, payload);

        // 2) 웹푸시 발송
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        // 구독 정보가 없으면(웹푸시 미구독) 보낼 대상이 없다.
        if (user.getPushEndpoint() == null || user.getPushP256dhKey() == null || user.getPushAuthKey() == null) {
            return;
        }
        // 알림을 꺼둔 유저에게는 보내지 않는다.
        if (!user.isPushEnabled()) {
            return;
        }

        try {
            nl.martijndwars.webpush.Notification notification = new nl.martijndwars.webpush.Notification(
                    user.getPushEndpoint(),
                    user.getPushP256dhKey(),
                    user.getPushAuthKey(),
                    payload.toJson()
            );
            var response = pushService.send(notification);
            int statusCode = response.getStatusLine().getStatusCode();

            // 410 Gone / 404: 구독이 만료·해지됨 → 저장된 구독 정보를 지운다(다음부터 시도 안 함).
            if (statusCode == HttpStatus.GONE.value() || statusCode == HttpStatus.NOT_FOUND.value()) {
                user.updatePushSubscription(null, null, null);
                userRepository.save(user);
                log.info("만료된 웹푸시 구독 정보 제거: userId={}", userId);
            } else if (statusCode >= 400) {
                log.warn("웹푸시 발송 실패: userId={}, status={}", userId, statusCode);
            }
        } catch (InterruptedException e) {
            // 인터럽트 플래그 복원: 호출 스레드의 중단 신호를 그대로 상위로 전달한다.
            Thread.currentThread().interrupt();
            log.warn("웹푸시 발송 중 인터럽트: userId={}", userId);
        } catch (Exception e) {
            // 알림 실패가 호출 도메인의 트랜잭션에 영향을 주지 않도록 예외를 삼킨다.
            log.warn("웹푸시 발송 중 예외: userId={}", userId, e);
        }
    }

    /**
     * 알림 이력을 notifications에 저장한다. 저장 실패가 발송이나 호출 도메인 트랜잭션에
     * 영향을 주지 않도록 예외를 삼키고 로깅만 한다.
     */
    private void saveNotification(Long userId, WebPushPayload payload) {
        try {
            notificationRepository.save(
                    Notification.create(userId, payload.title(), payload.body(), payload.category(), payload.url())
            );
        } catch (Exception e) {
            log.warn("알림 이력 저장 중 예외: userId={}", userId, e);
        }
    }
}
