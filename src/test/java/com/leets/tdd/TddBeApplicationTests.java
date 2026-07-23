package com.leets.tdd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mock;

// jwt.secret이 application.yaml에서 기본값 없이 필수(${JWT_SECRET})로 바뀌어서,
// 컨텍스트 로딩 테스트는 로컬 전용 fallback 값이 있는 application-local.yaml을 쓰도록
// local 프로필을 명시적으로 활성화한다(테스트/CI에서 JWT_SECRET을 직접 설정할 필요 없게).
@ActiveProfiles("local")
@SpringBootTest
@Import(TddBeApplicationTests.TestMailConfiguration.class)
class TddBeApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestMailConfiguration {

        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}