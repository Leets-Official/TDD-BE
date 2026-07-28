package com.leets.tdd.global.webpush;

import com.leets.tdd.user.domain.User;
import com.leets.tdd.user.repository.UserRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebPushSenderTest {

    @Mock
    private PushService pushService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WebPushSender webPushSender;

    private static final Long USER_ID = 1L;

    // web-push의 Notification 생성자는 p256dh를 실제 P-256 uncompressed point(0x04||X||Y, 65바이트)로
    // 디코딩해 EC 공개키를 만든다(내부: Utils.loadPublicKey → BouncyCastle ECCurve.decodePoint).
    // 임의 문자열을 넣으면 생성자에서 예외가 나 catch에 삼켜지고 send()가 호출되지 않으므로,
    // 테스트에서도 형식이 유효한 더미 키를 한 번 생성해 재사용한다.
    private static final String VALID_P256DH;
    private static final String VALID_AUTH;

    static {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            ECPublicKey pub = (ECPublicKey) kp.getPublic();
            ECPoint w = pub.getW();
            byte[] uncompressed = new byte[65];
            uncompressed[0] = 0x04;
            System.arraycopy(toUnsigned32(w.getAffineX()), 0, uncompressed, 1, 32);
            System.arraycopy(toUnsigned32(w.getAffineY()), 0, uncompressed, 33, 32);
            VALID_P256DH = Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressed);
            byte[] auth = new byte[16];
            new SecureRandom().nextBytes(auth);
            VALID_AUTH = Base64.getUrlEncoder().withoutPadding().encodeToString(auth);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static byte[] toUnsigned32(BigInteger bi) {
        byte[] raw = bi.toByteArray();
        byte[] out = new byte[32];
        if (raw.length == 33 && raw[0] == 0) {
            System.arraycopy(raw, 1, out, 0, 32);
        } else if (raw.length <= 32) {
            System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        } else {
            throw new IllegalStateException("좌표 길이 이상: " + raw.length);
        }
        return out;
    }

    private final WebPushPayload payload =
            new WebPushPayload("제목", "내용", "BOARD", "/posts/1");

    // protected 생성자로 빈 User를 만든 뒤 필드를 직접 심는다(엔티티라 Mock보다 실제 객체가 안정적).
    private User newUser(String endpoint, String p256dh, String auth, boolean pushEnabled) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            ReflectionTestUtils.setField(user, "pushEndpoint", endpoint);
            ReflectionTestUtils.setField(user, "pushP256dhKey", p256dh);
            ReflectionTestUtils.setField(user, "pushAuthKey", auth);
            ReflectionTestUtils.setField(user, "pushEnabled", pushEnabled);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("테스트용 User 생성 실패", e);
        }
    }

    private void stubSendWithStatus(int statusCode) throws Exception {
        HttpResponse response = org.mockito.Mockito.mock(HttpResponse.class);
        StatusLine statusLine = org.mockito.Mockito.mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(pushService.send(any(Notification.class))).thenReturn(response);
    }

    @Test
    @DisplayName("구독 정보가 있고 알림을 켠 유저에게는 웹푸시를 발송한다")
    void sendToUser_subscribed_sends() throws Exception {
        User user = newUser("https://push.example.com/ep", VALID_P256DH, VALID_AUTH, true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        stubSendWithStatus(201);

        webPushSender.sendToUser(USER_ID, payload);

        verify(pushService, times(1)).send(any(Notification.class));
    }

    @Test
    @DisplayName("구독 정보가 없는 유저에게는 발송하지 않는다")
    void sendToUser_noSubscription_skips() throws Exception {
        User user = newUser(null, null, null, true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        webPushSender.sendToUser(USER_ID, payload);

        verify(pushService, never()).send(any(Notification.class));
    }

    @Test
    @DisplayName("알림을 꺼둔 유저에게는 발송하지 않는다")
    void sendToUser_pushDisabled_skips() throws Exception {
        User user = newUser("https://push.example.com/ep", VALID_P256DH, VALID_AUTH, false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        webPushSender.sendToUser(USER_ID, payload);

        verify(pushService, never()).send(any(Notification.class));
    }

    @Test
    @DisplayName("410(만료) 응답을 받으면 구독 정보를 제거한다")
    void sendToUser_gone_removesSubscription() throws Exception {
        User user = newUser("https://push.example.com/ep", VALID_P256DH, VALID_AUTH, true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        stubSendWithStatus(410);

        webPushSender.sendToUser(USER_ID, payload);

        // 만료 시 구독 정보가 null로 초기화됐는지 (updatePushSubscription(null,null,null) 효과)
        assertThat(user.getPushEndpoint()).isNull();
        assertThat(user.getPushP256dhKey()).isNull();
        assertThat(user.getPushAuthKey()).isNull();
        verify(userRepository).save(user);
    }
}

