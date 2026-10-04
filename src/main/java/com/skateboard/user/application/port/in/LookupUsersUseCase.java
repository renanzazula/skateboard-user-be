package com.skateboard.user.application.port.in;

import com.skateboard.user.domain.model.IdentitySummary;

import java.util.List;
import java.util.UUID;

public interface LookupUsersUseCase {

    /** See IdentityProviderPort#findIdentities for the "unknown IDs are omitted" contract. */
    List<IdentitySummary> execute(List<UUID> keycloakUserIds);
}
