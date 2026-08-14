package com.skateboard.user.adapter.out.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * S3-compatible client for profile picture storage. Config keys mirror the
 * "railway.bucket" block already present (previously unused) in
 * skateboard-podcast-be's application-railway.yml.
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
                // path-style is required by most non-AWS S3-compatible endpoints
                // (Railway's bucket, MinIO, etc.) — virtual-hosted-style needs DNS
                // wildcarding those providers don't offer.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
