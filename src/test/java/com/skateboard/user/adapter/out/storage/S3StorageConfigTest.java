package com.skateboard.user.adapter.out.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3StorageConfig only wires SDK builders together, so building the beans with
 * known inputs and inspecting the resulting client/presigner configuration
 * (region, endpoint override, resolved credentials, and the actual shape of a
 * presigned URL) is the meaningful thing to verify here — no network calls are
 * made by either S3Client.builder().build() or by presigning a request.
 */
class S3StorageConfigTest {

    private static final String ACCESS_KEY_ID = "test-access-key";
    private static final String SECRET_ACCESS_KEY = "test-secret-key";
    private static final String ENDPOINT = "https://fake-bucket-endpoint.example.com";
    private static final String REGION = "us-east-1";

    private final S3StorageConfig config = new S3StorageConfig();

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @AfterEach
    void tearDown() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }

    @Test
    void s3ClientBeanIsConfiguredWithGivenEndpointRegionAndCredentials() {
        s3Client = config.s3Client(ACCESS_KEY_ID, SECRET_ACCESS_KEY, ENDPOINT, REGION);

        assertThat(s3Client).isNotNull();
        S3ServiceClientConfiguration clientConfig = s3Client.serviceClientConfiguration();
        assertThat(clientConfig.region()).isEqualTo(Region.of(REGION));
        assertThat(clientConfig.endpointOverride()).contains(URI.create(ENDPOINT));

        AwsCredentialsIdentity credentials = clientConfig.credentialsProvider().resolveIdentity().join();
        assertThat(credentials.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        assertThat(credentials.secretAccessKey()).isEqualTo(SECRET_ACCESS_KEY);
    }

    @Test
    void s3PresignerBeanIsConfiguredWithGivenEndpointRegionAndCredentials() {
        s3Presigner = config.s3Presigner(ACCESS_KEY_ID, SECRET_ACCESS_KEY, ENDPOINT, REGION);

        assertThat(s3Presigner).isNotNull();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(b -> b.bucket("my-bucket").key("profile-pictures/photo.png"))
                        .build());

        URI endpointUri = URI.create(ENDPOINT);
        assertThat(presigned.httpRequest().host()).isEqualTo(endpointUri.getHost());
        // Path-style access (S3Configuration.pathStyleAccessEnabled(true)) puts the
        // bucket in the URL path rather than as a virtual-hosted subdomain — this is
        // required for Railway/MinIO endpoints, so assert it actually took effect.
        assertThat(presigned.httpRequest().encodedPath()).isEqualTo("/my-bucket/profile-pictures/photo.png");
        assertThat(presigned.httpRequest().rawQueryParameters().get("X-Amz-Expires"))
                .containsExactly(String.valueOf(Duration.ofMinutes(5).toSeconds()));
        assertThat(presigned.httpRequest().rawQueryParameters().get("X-Amz-Credential").get(0))
                .contains(ACCESS_KEY_ID);
    }
}
