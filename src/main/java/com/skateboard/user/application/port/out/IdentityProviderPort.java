package com.skateboard.user.application.port.out;

import java.util.UUID;

/**
 * Boundary to Keycloak's Admin API. Keycloak-specific types (representations,
 * the admin client itself) must never leak past this interface — see
 * adapter/out/keycloak/KeycloakIdentityAdapter.
 */
public interface IdentityProviderPort {

    void changeUsername(UUID keycloakUserId, String newUsername);

    /** Disables the identity and best-effort revokes active sessions/refresh tokens. */
    void disableIdentity(UUID keycloakUserId);

    /** Idempotent — a no-op if the identity is already gone. */
    void deleteIdentity(UUID keycloakUserId);

    void resetPassword(UUID keycloakUserId, String newPassword);
}
