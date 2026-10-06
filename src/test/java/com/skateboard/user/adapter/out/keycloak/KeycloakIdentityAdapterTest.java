package com.skateboard.user.adapter.out.keycloak;

import com.skateboard.user.domain.model.IdentitySummary;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeycloakIdentityAdapterTest {

    private static final String REALM = "skateboard-podcast";
    private static final String DEFAULT_TENANT_ID = "default-tenant-id";

    @Mock
    private Keycloak keycloakAdminClient;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    private KeycloakIdentityAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new KeycloakIdentityAdapter(keycloakAdminClient, REALM, DEFAULT_TENANT_ID);
        when(keycloakAdminClient.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    private void stubUserResource(UUID keycloakUserId) {
        when(usersResource.get(keycloakUserId.toString())).thenReturn(userResource);
    }

    @Test
    void changeUsernameUpdatesRepresentationAndPersistsIt() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);
        UserRepresentation representation = new UserRepresentation();
        representation.setUsername("old-username");
        when(userResource.toRepresentation()).thenReturn(representation);

        adapter.changeUsername(keycloakUserId, "new-username");

        assertThat(representation.getUsername()).isEqualTo("new-username");
        verify(userResource).update(representation);
    }

    @Test
    void disableIdentityDisablesUserAndRevokesSessions() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);
        UserRepresentation representation = new UserRepresentation();
        representation.setEnabled(true);
        when(userResource.toRepresentation()).thenReturn(representation);

        adapter.disableIdentity(keycloakUserId);

        assertThat(representation.isEnabled()).isFalse();
        verify(userResource).update(representation);
        verify(userResource).logout();
    }

    @Test
    void disableIdentitySwallowsLogoutFailureAsBestEffort() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);
        UserRepresentation representation = new UserRepresentation();
        representation.setEnabled(true);
        when(userResource.toRepresentation()).thenReturn(representation);
        doThrow(new RuntimeException("session revocation unavailable")).when(userResource).logout();

        assertThatCode(() -> adapter.disableIdentity(keycloakUserId)).doesNotThrowAnyException();

        assertThat(representation.isEnabled()).isFalse();
        verify(userResource).update(representation);
        verify(userResource).logout();
    }

    @Test
    void deleteIdentityDeletesUserFromRealm() {
        UUID keycloakUserId = UUID.randomUUID();

        adapter.deleteIdentity(keycloakUserId);

        verify(usersResource).delete(keycloakUserId.toString());
    }

    @Test
    void deleteIdentityIsIdempotentWhenUserAlreadyGone() {
        UUID keycloakUserId = UUID.randomUUID();
        when(usersResource.delete(keycloakUserId.toString())).thenThrow(new NotFoundException());

        assertThatCode(() -> adapter.deleteIdentity(keycloakUserId)).doesNotThrowAnyException();

        verify(usersResource).delete(keycloakUserId.toString());
    }

    @Test
    void resetPasswordSetsPermanentPasswordCredential() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);

        adapter.resetPassword(keycloakUserId, "s3cret-Passw0rd");

        ArgumentCaptor<CredentialRepresentation> captor = ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(captor.capture());
        CredentialRepresentation credential = captor.getValue();
        assertThat(credential.getType()).isEqualTo(CredentialRepresentation.PASSWORD);
        assertThat(credential.getValue()).isEqualTo("s3cret-Passw0rd");
        assertThat(credential.isTemporary()).isFalse();
    }

    @Test
    void ensureTenantAssignedBackfillsDefaultTenantWhenAttributesMapIsNull() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);
        UserRepresentation representation = new UserRepresentation();
        representation.setAttributes(null);
        when(userResource.toRepresentation()).thenReturn(representation);

        adapter.ensureTenantAssigned(keycloakUserId);

        assertThat(representation.getAttributes()).containsEntry("tenant_id", List.of(DEFAULT_TENANT_ID));
        verify(userResource).update(representation);
    }

    @Test
    void ensureTenantAssignedBackfillsDefaultTenantWhenAttributeMissing() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);
        UserRepresentation representation = new UserRepresentation();
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("some-other-attribute", List.of("value"));
        representation.setAttributes(attributes);
        when(userResource.toRepresentation()).thenReturn(representation);

        adapter.ensureTenantAssigned(keycloakUserId);

        assertThat(representation.getAttributes()).containsEntry("tenant_id", List.of(DEFAULT_TENANT_ID));
        verify(userResource).update(representation);
    }

    @Test
    void ensureTenantAssignedTreatsBlankExistingTenantAsMissing() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);
        UserRepresentation representation = new UserRepresentation();
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("tenant_id", List.of(""));
        representation.setAttributes(attributes);
        when(userResource.toRepresentation()).thenReturn(representation);

        adapter.ensureTenantAssigned(keycloakUserId);

        assertThat(representation.getAttributes()).containsEntry("tenant_id", List.of(DEFAULT_TENANT_ID));
        verify(userResource).update(representation);
    }

    @Test
    void ensureTenantAssignedTreatsEmptyExistingListAsMissing() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);
        UserRepresentation representation = new UserRepresentation();
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("tenant_id", List.of());
        representation.setAttributes(attributes);
        when(userResource.toRepresentation()).thenReturn(representation);

        adapter.ensureTenantAssigned(keycloakUserId);

        assertThat(representation.getAttributes()).containsEntry("tenant_id", List.of(DEFAULT_TENANT_ID));
        verify(userResource).update(representation);
    }

    @Test
    void ensureTenantAssignedNeverOverwritesAnExistingTenant() {
        UUID keycloakUserId = UUID.randomUUID();
        stubUserResource(keycloakUserId);
        UserRepresentation representation = new UserRepresentation();
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("tenant_id", List.of("already-assigned-tenant"));
        representation.setAttributes(attributes);
        when(userResource.toRepresentation()).thenReturn(representation);

        adapter.ensureTenantAssigned(keycloakUserId);

        assertThat(representation.getAttributes()).containsEntry("tenant_id", List.of("already-assigned-tenant"));
        verify(userResource, never()).update(any());
    }

    @Test
    void findIdentitiesResolvesEachIdToAnIdentitySummary() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UserResource firstResource = mock(UserResource.class);
        UserResource secondResource = mock(UserResource.class);
        when(usersResource.get(first.toString())).thenReturn(firstResource);
        when(usersResource.get(second.toString())).thenReturn(secondResource);

        UserRepresentation firstRepresentation = new UserRepresentation();
        firstRepresentation.setEmail("first@example.com");
        firstRepresentation.setEmailVerified(true);
        firstRepresentation.setEnabled(true);
        when(firstResource.toRepresentation()).thenReturn(firstRepresentation);

        UserRepresentation secondRepresentation = new UserRepresentation();
        secondRepresentation.setEmail("second@example.com");
        secondRepresentation.setEmailVerified(false);
        secondRepresentation.setEnabled(false);
        when(secondResource.toRepresentation()).thenReturn(secondRepresentation);

        List<IdentitySummary> result = adapter.findIdentities(List.of(first, second));

        assertThat(result).containsExactly(
                new IdentitySummary(first, "first@example.com", true, true),
                new IdentitySummary(second, "second@example.com", false, false));
    }

    @Test
    void findIdentitiesOmitsIdsWithNoMatchingIdentity() {
        UUID missing = UUID.randomUUID();
        when(usersResource.get(missing.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException());

        List<IdentitySummary> result = adapter.findIdentities(List.of(missing));

        assertThat(result).isEmpty();
    }
}
