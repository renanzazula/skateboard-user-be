package com.skateboard.user.application.port.out;

import java.io.InputStream;
import java.util.UUID;

public interface ProfileImageStoragePort {

    StoredImage upload(UUID keycloakUserId, String filename, String contentType, InputStream content, long contentLength);

    /**
     * A fresh, time-limited GET URL for an already-uploaded object. Objects
     * are private (see S3ProfileImageStorageAdapter), so this — not a stored
     * URL — is the only way to actually read one back. Returns null if
     * objectKey is null/blank (no picture uploaded yet).
     */
    String presignGetUrl(String objectKey);

    /** Best-effort — callers should not fail the overall operation if this fails. */
    void delete(String objectKey);

    record StoredImage(String objectKey, String url) {}
}
