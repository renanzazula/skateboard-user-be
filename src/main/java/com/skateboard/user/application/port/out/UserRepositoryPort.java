package com.skateboard.user.application.port.out;

import com.skateboard.user.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<UserProfile> findByKeycloakUserId(UUID keycloakUserId);
    UserProfile save(UserProfile profile);
}
