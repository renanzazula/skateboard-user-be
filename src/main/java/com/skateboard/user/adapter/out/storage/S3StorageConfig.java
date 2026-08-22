package com.skateboard.user.adapter.out.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3-compatible client for profile picture storage. Config keys mirror the
 * "railway.bucket" block already present (previously unused) in
 * skateboard-podcast-be's application-railway.yml, and this setup mirrors
 * skateboard-app-config-be's S3StorageConfig — same Tigris-backed bucket.
 */
@Configuration
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(
            @Value("${railway.bucket.access-key-id}") String accessKeyId,
            @Value("${railway.bucket.secret-access-key}") String secretAccessKey,
            @Value("${railway.bucket.endpoint}") String endpoint,
            @Value("${railway.bucket.region}") String region) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(s3Configuration())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${railway.bucket.access-key-id}") String accessKeyId,
            @Value("${railway.bucket.secret-access-key}") String secretAccessKey,
            @Value("${railway.bucket.endpoint}") String endpoint,
            @Value("${railway.bucket.region}") String region) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .serviceConfiguration(s3Configuration())
                .build();
    }

    private S3Configuration s3Configuration() {
        return S3Configuration.builder()
                // path-style is required by most non-AWS S3-compatible endpoints
                // (Railway's bucket, MinIO, etc.) — virtual-hosted-style needs DNS
                // wildcarding those providers don't offer.
                .pathStyleAccessEnabled(true)
                // Railway Bucket (like most non-AWS S3-compatible stores) doesn't
                // implement AWS's MD5/CRC checksum trailer semantics — leaving this
                // enabled (the SDK default) makes PutObject/GetObject calls fail.
                .checksumValidationEnabled(false)
                .build();
    }
}
