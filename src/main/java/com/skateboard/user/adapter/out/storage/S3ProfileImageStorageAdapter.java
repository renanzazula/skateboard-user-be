package com.skateboard.user.adapter.out.storage;

import com.skateboard.user.application.port.out.ProfileImageStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Component
public class S3ProfileImageStorageAdapter implements ProfileImageStoragePort {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicBaseUrl;

    public S3ProfileImageStorageAdapter(S3Client s3Client,
                                         @Value("${railway.bucket.bucket-name}") String bucketName,
                                         @Value("${railway.bucket.endpoint}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
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
        String url = "%s/%s/%s".formatted(publicBaseUrl, bucketName, objectKey);
        return new StoredImage(objectKey, url);
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
