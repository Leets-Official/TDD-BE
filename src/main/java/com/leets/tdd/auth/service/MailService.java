package com.leets.tdd.auth.service;

public interface MailService {
    void sendVerificationCode(String email, String code);
}
