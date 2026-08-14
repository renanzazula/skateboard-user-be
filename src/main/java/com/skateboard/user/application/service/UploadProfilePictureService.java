package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.UploadProfilePictureUseCase;
import com.skateboard.user.application.port.out.ProfileImageStoragePort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UploadProfilePictureService implements UploadProfilePictureUseCase {

    private static final Logger log = LoggerFactory.getLogger(UploadProfilePictureService.class);

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UserRepositoryPort userRepositoryPort;
    private final ProfileImageStoragePort profileImageStoragePort;

    public UploadProfilePictureService(GetCurrentUserUseCase getCurrentUserUseCase,
                                        UserRepositoryPort userRepositoryPort,
                                        ProfileImageStoragePort profileImageStoragePort) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.userRepositoryPort = userRepositoryPort;
        this.profileImageStoragePort = profileImageStoragePort;
    }

    @Override
    public UserProfile execute(Input input) {
        UserProfile profile = getCurrentUserUseCase.execute(input.keycloakUserId(), null);

        String previousObjectKey = profile.getProfilePictureObjectKey();
        ProfileImageStoragePort.StoredImage stored = profileImageStoragePort.upload(
                input.keycloakUserId(), input.filename(), input.contentType(), input.content(), input.contentLength());
        profile.updateProfilePicture(stored.url(), stored.objectKey());
        UserProfile saved = userRepositoryPort.save(profile);

        if (previousObjectKey != null) {
            try {
                profileImageStoragePort.delete(previousObjectKey);
            } catch (Exception e) {
                log.warn("Failed to delete previous profile picture object {}", previousObjectKey, e);
            }
        }
        return saved;
    }
}
