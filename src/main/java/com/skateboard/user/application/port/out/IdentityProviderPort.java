package com.skateboard.user.application.port.out;

import com.skateboard.user.domain.model.IdentitySummary;

import java.util.List;
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

    /**
     * Backfills the {@code tenant_id} attribute (see skateboard-podcast
     * realm-export.json's userProfile config) with the platform's shared
     * default tenant if the user doesn't already have one — a no-op
     * otherwise. Needed because neither self-registration nor a Google
     * first-login populates it (the field is admin-only, so it's never on
     * a form a regular user submits).
     */
    void ensureTenantAssigned(UUID keycloakUserId);

    /**
     * Resolves each given Keycloak user ID to its email/verification/active
     * status, for admin-only recipient validation (e.g. Guest Application
     * notification recipients — see AdminUserController). IDs with no
     * matching identity are omitted from the result rather than causing an
     * error, since a stale/removed recipient ID is an expected admin-config
     * drift case, not a caller bug.
     */
    List<IdentitySummary> findIdentities(List<UUID> keycloakUserIds);
}
