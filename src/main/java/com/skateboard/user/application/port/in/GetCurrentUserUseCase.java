package com.skateboard.user.application.port.in;

import com.skateboard.user.domain.model.UserProfile;

import java.util.UUID;

public interface GetCurrentUserUseCase {

    /**
     * Returns the caller's profile, provisioning a new row on first access.
     * Keycloak owns identity creation and this service has no separate
     * registration/webhook flow, so the app-side row is created lazily the
     * first time an authenticated caller is seen.
     */
    UserProfile execute(UUID keycloakUserId, String usernameHint);
}
