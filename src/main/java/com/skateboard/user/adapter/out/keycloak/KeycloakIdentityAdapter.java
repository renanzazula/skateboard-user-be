package com.skateboard.user.adapter.out.keycloak;

import com.skateboard.user.application.port.out.IdentityProviderPort;
import jakarta.ws.rs.NotFoundException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Only class in the service that touches Keycloak's Admin API — the
 * IdentityProviderPort is what the application layer depends on, per the
 * README's "Keycloak Admin API details must not leak into the frontend or
 * BFF" rule (which applies just as much to leaking out of this adapter).
 */
@Component
public class KeycloakIdentityAdapter implements IdentityProviderPort {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityAdapter.class);

    private static final String TENANT_ID_ATTRIBUTE = "tenant_id";

    private final Keycloak keycloakAdminClient;
    private final String realm;
    private final String defaultTenantId;

    public KeycloakIdentityAdapter(Keycloak keycloakAdminClient,
                                    @Value("${app.security.oauth2.admin.realm}") String realm,
                                    @Value("${app.tenancy.default-tenant-id}") String defaultTenantId) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.realm = realm;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public void changeUsername(UUID keycloakUserId, String newUsername) {
        UserResource resource = userResource(keycloakUserId);
        UserRepresentation representation = resource.toRepresentation();
        representation.setUsername(newUsername);
        resource.update(representation);
    }

    @Override
    public void disableIdentity(UUID keycloakUserId) {
        UserResource resource = userResource(keycloakUserId);
        UserRepresentation representation = resource.toRepresentation();
        representation.setEnabled(false);
        resource.update(representation);
        try {
            resource.logout();
        } catch (Exception e) {
            log.warn("Failed to revoke sessions for deactivated user {}", keycloakUserId, e);
        }
    }

    @Override
    public void deleteIdentity(UUID keycloakUserId) {
        try {
            keycloakAdminClient.realm(realm).users().delete(keycloakUserId.toString());
        } catch (NotFoundException e) {
            // already deleted — deleteIdentity is idempotent
        }
    }

    @Override
    public void resetPassword(UUID keycloakUserId, String newPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);
        userResource(keycloakUserId).resetPassword(credential);
    }

    @Override
    public void ensureTenantAssigned(UUID keycloakUserId) {
        UserResource resource = userResource(keycloakUserId);
        UserRepresentation representation = resource.toRepresentation();
        Map<String, List<String>> attributes = representation.getAttributes();
        List<String> existing = attributes == null ? null : attributes.get(TENANT_ID_ATTRIBUTE);
        if (existing != null && !existing.isEmpty() && !existing.get(0).isBlank()) {
            return; // already has a tenant — never overwrite
        }
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(TENANT_ID_ATTRIBUTE, List.of(defaultTenantId));
        representation.setAttributes(attributes);
        resource.update(representation);
    }

    private UserResource userResource(UUID keycloakUserId) {
        return keycloakAdminClient.realm(realm).users().get(keycloakUserId.toString());
    }
}
