package com.skateboard.user.application.port.in;

import com.skateboard.user.domain.model.UserProfile;

import java.util.UUID;

public interface ChangeUsernameUseCase {

    UserProfile execute(Input input);

    record Input(UUID keycloakUserId, String newUsername) {}
}
