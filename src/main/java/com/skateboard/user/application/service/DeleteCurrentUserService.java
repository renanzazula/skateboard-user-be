package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.DeleteCurrentUserUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.AccountStatus;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeleteCurrentUserService implements DeleteCurrentUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final IdentityProviderPort identityProviderPort;

    public DeleteCurrentUserService(UserRepositoryPort userRepositoryPort, IdentityProviderPort identityProviderPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    public void execute(UUID keycloakUserId) {
        // A missing local row does NOT mean "nothing to delete" — it only
        // means this account was never provisioned app-side (e.g. the
        // caller went straight to Delete without ever calling GET /me). The
        // Keycloak identity is the authoritative account and must still be
        // deleted. Only a row we know for certain was already processed
        // (status DELETED) short-circuits, for real idempotency.
        Optional<UserProfile> existing = userRepositoryPort.findByKeycloakUserId(keycloakUserId);
        if (existing.isPresent()) {
            UserProfile profile = existing.get();
            if (profile.getStatus() == AccountStatus.DELETED) {
                return;
            }
            profile.markDeleted();
            userRepositoryPort.save(profile);
        }
        identityProviderPort.deleteIdentity(keycloakUserId);
    }
}
