package com.skateboard.user.domain.model;

import java.util.UUID;

/**
 * Projection of a Keycloak identity for admin-only bulk lookups (see
 * IdentityProviderPort#findIdentities). Deliberately thinner than a full
 * UserProfile — callers like Guest Application recipient validation only
 * ever need email/verification/active status, never the rest of the
 * profile.
 */
public record IdentitySummary(UUID keycloakUserId, String email, boolean emailVerified, boolean active) {
}
