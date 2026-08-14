package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.ChangeUsernameUseCase;
import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class ChangeUsernameService implements ChangeUsernameUseCase {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UserRepositoryPort userRepositoryPort;
    private final IdentityProviderPort identityProviderPort;

    public ChangeUsernameService(GetCurrentUserUseCase getCurrentUserUseCase, UserRepositoryPort userRepositoryPort,
                                  IdentityProviderPort identityProviderPort) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.userRepositoryPort = userRepositoryPort;
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    public UserProfile execute(Input input) {
        UserProfile profile = getCurrentUserUseCase.execute(input.keycloakUserId(), null);

        // Keycloak first: it's the source of truth for uniqueness, so a
        // rejected change (e.g. username already taken) never gets recorded
        // in our copy.
        identityProviderPort.changeUsername(input.keycloakUserId(), input.newUsername());
        profile.changeUsername(input.newUsername());
        return userRepositoryPort.save(profile);
    }
}
