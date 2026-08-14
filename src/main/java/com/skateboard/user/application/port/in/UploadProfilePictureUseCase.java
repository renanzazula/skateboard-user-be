package com.skateboard.user.application.port.in;

import com.skateboard.user.domain.model.UserProfile;

import java.io.InputStream;
import java.util.UUID;

public interface UploadProfilePictureUseCase {

    UserProfile execute(Input input);

    record Input(UUID keycloakUserId, String filename, String contentType, InputStream content, long contentLength) {}
}
