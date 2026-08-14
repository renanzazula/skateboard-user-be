package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.DeactivateCurrentUserUseCase;
import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeactivateCurrentUserService implements DeactivateCurrentUserUseCase {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UserRepositoryPort userRepositoryPort;
    private final IdentityProviderPort identityProviderPort;

    public DeactivateCurrentUserService(GetCurrentUserUseCase getCurrentUserUseCase, UserRepositoryPort userRepositoryPort,
                                         IdentityProviderPort identityProviderPort) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.userRepositoryPort = userRepositoryPort;
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    public UserProfile execute(UUID keycloakUserId) {
        UserProfile profile = getCurrentUserUseCase.execute(keycloakUserId, null);
        profile.deactivate();
        UserProfile saved = userRepositoryPort.save(profile);
        // Disables the Keycloak identity and best-effort revokes active
        // sessions/refresh tokens. Already-issued short-lived access tokens
        // remain valid until they expire — a standard OAuth2 limitation, not
        // something this call can fix.
        identityProviderPort.disableIdentity(keycloakUserId);
        return saved;
    }
}
