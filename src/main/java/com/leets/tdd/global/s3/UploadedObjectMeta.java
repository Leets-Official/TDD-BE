package com.leets.tdd.global.s3;

/**
 * S3 HeadObject 응답에서 확정(confirm) 검증에 필요한 필드만 추린 값.
 * 비공개(ImageStorageService)/공개(ProfileImageStorageService) 버킷 양쪽에서 공용으로 쓴다.
 */
public record UploadedObjectMeta(long contentLength, String contentType) {
}
