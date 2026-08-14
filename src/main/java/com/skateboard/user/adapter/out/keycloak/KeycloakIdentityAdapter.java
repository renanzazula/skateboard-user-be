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

    private final Keycloak keycloakAdminClient;
    private final String realm;

    public KeycloakIdentityAdapter(Keycloak keycloakAdminClient,
                                    @Value("${app.security.oauth2.admin.realm}") String realm) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.realm = realm;
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

    private UserResource userResource(UUID keycloakUserId) {
        return keycloakAdminClient.realm(realm).users().get(keycloakUserId.toString());
    }
}
