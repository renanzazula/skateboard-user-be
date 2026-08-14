package com.skateboard.user.application.port.out;

import java.io.InputStream;
import java.util.UUID;

public interface ProfileImageStoragePort {

    StoredImage upload(UUID keycloakUserId, String filename, String contentType, InputStream content, long contentLength);

    /** Best-effort — callers should not fail the overall operation if this fails. */
    void delete(String objectKey);

    record StoredImage(String objectKey, String url) {}
}
