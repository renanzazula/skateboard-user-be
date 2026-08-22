package com.skateboard.user.adapter.out.storage;

import com.skateboard.user.application.port.out.ProfileImageStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Component
public class S3ProfileImageStorageAdapter implements ProfileImageStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final long presignedUrlExpirationMinutes;

    public S3ProfileImageStorageAdapter(S3Client s3Client,
                                         S3Presigner s3Presigner,
                                         @Value("${railway.bucket.bucket-name}") String bucketName,
                                         @Value("${railway.bucket.presigned-url-expiration-minutes}") long presignedUrlExpirationMinutes) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.presignedUrlExpirationMinutes = presignedUrlExpirationMinutes;
    }

    @Override
    public StoredImage upload(UUID keycloakUserId, String filename, String contentType, InputStream content, long contentLength) {
        String objectKey = "profile-pictures/%s/%s-%s".formatted(keycloakUserId, UUID.randomUUID(), sanitize(filename));
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build(),
                RequestBody.fromInputStream(content, contentLength));
        // Objects are private — Railway's Tigris-backed bucket doesn't honor
        // per-object canned ACLs the way AWS S3 does, so a direct/unsigned URL
        // here would 403 (confirmed: every profile picture did). The URL
        // callers actually see always comes from presignGetUrl() at read time.
        return new StoredImage(objectKey, presignGetUrl(objectKey));
    }

    @Override
    public String presignGetUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(objectKey).build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void delete(String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build());
    }

    /** Strips path separators so a crafted filename can't escape the per-user prefix. */
    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "upload";
        return filename.replaceAll("[/\\\\]", "_");
    }
}
