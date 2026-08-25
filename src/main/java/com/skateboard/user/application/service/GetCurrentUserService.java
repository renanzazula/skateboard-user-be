package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetCurrentUserService implements GetCurrentUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final IdentityProviderPort identityProviderPort;

    public GetCurrentUserService(UserRepositoryPort userRepositoryPort, IdentityProviderPort identityProviderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    public UserProfile execute(UUID keycloakUserId, String usernameHint) {
        return userRepositoryPort.findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> {
                    // First time this Keycloak identity is seen locally — also the
                    // first opportunity to backfill tenant_id for users who never
                    // went through a flow that sets it (self-registration, Google
                    // first-login). See IdentityProviderPort#ensureTenantAssigned.
                    identityProviderPort.ensureTenantAssigned(keycloakUserId);
                    return userRepositoryPort.save(UserProfile.provision(keycloakUserId, usernameHint));
                });
    }
}
