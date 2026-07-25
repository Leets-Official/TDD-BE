package com.leets.tdd.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3")
public record S3Properties(
        String privateBucket,
        String publicBucket,
        String publicBaseUrl,
        String region,
        long presignedUrlValiditySeconds,
        long maxFileSizeBytes
) {
}
