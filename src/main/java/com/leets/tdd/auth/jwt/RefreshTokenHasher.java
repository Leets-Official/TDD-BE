package com.leets.tdd.auth.jwt;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

//refresh token(JWT) 저장용 단방향 해시.
@Component
public class RefreshTokenHasher {

    private static final String ALGORITHM = "SHA-256";

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("해시 알고리즘을 사용할 수 없습니다: " + ALGORITHM, e);
        }
    }
}
