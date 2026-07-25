package com.leets.tdd.auth.service;

import com.leets.tdd.auth.exception.AuthErrorCode;
import com.leets.tdd.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[TDD] 이메일 인증코드 안내");
        message.setText("인증코드는 %s 입니다. 5분 내에 입력해주세요.".formatted(code));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("이메일 발송 실패: {}", email, e);
            throw new AuthException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
