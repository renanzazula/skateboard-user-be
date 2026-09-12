package com.skateboard.user.adapter.out.storage;

import com.skateboard.user.application.port.out.ProfileImageStoragePort.StoredImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class S3ProfileImageStorageAdapterTest {

    private static final String BUCKET_NAME = "skateboard-user";
    private static final long PRESIGNED_URL_EXPIRATION_MINUTES = 60L;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private S3ProfileImageStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new S3ProfileImageStorageAdapter(s3Client, s3Presigner, BUCKET_NAME, PRESIGNED_URL_EXPIRATION_MINUTES);
    }

    @Test
    void uploadStoresObjectUnderPerUserPrefixAndReturnsFreshPresignedUrl() throws MalformedURLException {
        UUID keycloakUserId = UUID.randomUUID();
        InputStream content = new ByteArrayInputStream("image-bytes".getBytes());
        URL presignedUrl = new URL("https://fake-bucket-endpoint.example.com/" + BUCKET_NAME + "/some-key?X-Amz-Expires=3600");
        when(presignedGetObjectRequest.url()).thenReturn(presignedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        StoredImage result = adapter.upload(keycloakUserId, "avatar.png", "image/png", content, 11L);

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(putRequestCaptor.capture(), requestBodyCaptor.capture());

        PutObjectRequest putRequest = putRequestCaptor.getValue();
        assertThat(putRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(putRequest.contentType()).isEqualTo("image/png");
        assertThat(putRequest.contentLength()).isEqualTo(11L);
        assertThat(putRequest.key())
                .startsWith("profile-pictures/" + keycloakUserId + "/")
                .endsWith("-avatar.png");
        assertThat(requestBodyCaptor.getValue().optionalContentLength()).hasValue(11L);

        // presignGetUrl() must be called for the freshly-uploaded key, not a cached/stored URL.
        ArgumentCaptor<GetObjectPresignRequest> presignCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(presignCaptor.capture());
        GetObjectRequest signedGetRequest = presignCaptor.getValue().getObjectRequest();
        assertThat(signedGetRequest.bucket()).isEqualTo(BUCKET_NAME);
        assertThat(signedGetRequest.key()).isEqualTo(putRequest.key());
        assertThat(presignCaptor.getValue().signatureDuration())
                .isEqualTo(Duration.ofMinutes(PRESIGNED_URL_EXPIRATION_MINUTES));

        assertThat(result.objectKey()).isEqualTo(putRequest.key());
        assertThat(result.url()).isEqualTo(presignedUrl.toString());
    }

    @Test
    void uploadSanitizesFilenamesContainingPathSeparators() throws MalformedURLException {
        UUID keycloakUserId = UUID.randomUUID();
        when(presignedGetObjectRequest.url()).thenReturn(new URL("https://fake-bucket-endpoint.example.com/x"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        adapter.upload(keycloakUserId, "../../etc\\passwd.png", "image/png", new ByteArrayInputStream(new byte[0]), 0L);

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putRequestCaptor.capture(), any(RequestBody.class));
        assertThat(putRequestCaptor.getValue().key())
                .doesNotContain("..\\", "../")
                .endsWith("-.._.._etc_passwd.png");
    }

    @Test
    void uploadFallsBackToDefaultNameWhenFilenameIsBlank() throws MalformedURLException {
        UUID keycloakUserId = UUID.randomUUID();
        when(presignedGetObjectRequest.url()).thenReturn(new URL("https://fake-bucket-endpoint.example.com/x"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        adapter.upload(keycloakUserId, "   ", "image/png", new ByteArrayInputStream(new byte[0]), 0L);

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putRequestCaptor.capture(), any(RequestBody.class));
        assertThat(putRequestCaptor.getValue().key()).endsWith("-upload");
    }

    @Test
    void uploadPropagatesS3ExceptionsWithoutTranslation() {
        UUID keycloakUserId = UUID.randomUUID();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("bucket unreachable").build());

        assertThatThrownBy(() -> adapter.upload(keycloakUserId, "avatar.png", "image/png",
                new ByteArrayInputStream(new byte[0]), 0L))
                .isInstanceOf(S3Exception.class)
                .hasMessageContaining("bucket unreachable");

        // Failed upload must never attempt to presign a URL for an object that isn't there.
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void presignGetUrlReturnsNullForNullObjectKeyWithoutCallingPresigner() {
        assertThat(adapter.presignGetUrl(null)).isNull();
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void presignGetUrlReturnsNullForBlankObjectKeyWithoutCallingPresigner() {
        assertThat(adapter.presignGetUrl("   ")).isNull();
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void presignGetUrlAlwaysBuildsFreshRequestWithConfiguredBucketKeyAndExpiry() throws MalformedURLException {
        String objectKey = "profile-pictures/user-1/photo.png";
        URL expectedUrl = new URL("https://fake-bucket-endpoint.example.com/" + BUCKET_NAME + "/" + objectKey);
        when(presignedGetObjectRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        String url = adapter.presignGetUrl(objectKey);

        assertThat(url).isEqualTo(expectedUrl.toString());
        ArgumentCaptor<GetObjectPresignRequest> presignCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(presignCaptor.capture());
        GetObjectPresignRequest presignRequest = presignCaptor.getValue();
        assertThat(presignRequest.signatureDuration()).isEqualTo(Duration.ofMinutes(PRESIGNED_URL_EXPIRATION_MINUTES));
        assertThat(presignRequest.getObjectRequest().bucket()).isEqualTo(BUCKET_NAME);
        assertThat(presignRequest.getObjectRequest().key()).isEqualTo(objectKey);
    }

    @Test
    void deleteRemovesObjectByBucketAndKey() {
        String objectKey = "profile-pictures/user-1/photo.png";

        adapter.delete(objectKey);

        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().bucket()).isEqualTo(BUCKET_NAME);
        assertThat(deleteCaptor.getValue().key()).isEqualTo(objectKey);
    }

    @Test
    void deletePropagatesS3ExceptionsToTheCaller() {
        String objectKey = "profile-pictures/user-1/photo.png";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("delete failed").build());

        assertThatThrownBy(() -> adapter.delete(objectKey)).isInstanceOf(S3Exception.class);
    }
}
