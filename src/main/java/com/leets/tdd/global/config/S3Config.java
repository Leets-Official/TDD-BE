package com.leets.tdd.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 업로드(기숙사 인증 이미지) + Presigned URL 발급용 클라이언트.
 * DefaultCredentialsProvider를 사용하므로 액세스 키를 코드/설정에 직접 넣지 않는다.
 * - 운영 EC2: 인스턴스에 연결된 IAM 역할(tdd-ec2-s3-assets-role)을 자동으로 사용한다.
 * - 로컬 개발: 환경변수(AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY) 또는 ~/.aws/credentials를 사용한다.
 * region은 deploy.yml이 심어주는 AWS_REGION 환경변수를 그대로 읽는다(PR #71).
 * 버킷 자체는 비공개(private)이고, 공개 URL을 만들지 않는다 - 조회는 항상 짧은 만료 시간의
 * Presigned GET URL로만 발급한다(ImageStorageService 참고). 프로필 이미지용 공개 버킷은
 * 이 클라이언트 범위 밖이다.
 */
@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
