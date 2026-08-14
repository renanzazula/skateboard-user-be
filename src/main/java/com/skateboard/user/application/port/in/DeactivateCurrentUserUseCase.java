package com.skateboard.user.application.port.in;

import com.skateboard.user.domain.model.UserProfile;

import java.util.UUID;

public interface DeactivateCurrentUserUseCase {
    UserProfile execute(UUID keycloakUserId);
}
