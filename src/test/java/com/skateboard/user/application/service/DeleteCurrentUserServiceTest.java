package com.skateboard.user.application.service;

import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.AccountStatus;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteCurrentUserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private IdentityProviderPort identityProviderPort;

    private DeleteCurrentUserService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DeleteCurrentUserService(userRepositoryPort, identityProviderPort);
    }

    @Test
    void anonymizesProfileAndDeletesIdentity() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.updateDisplayName("Rzazula");
        when(userRepositoryPort.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(profile));

        service.execute(keycloakUserId);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(captor.getValue().getDisplayName()).isNull();
        verify(identityProviderPort).deleteIdentity(keycloakUserId);
    }

    @Test
    void missingLocalProfileStillDeletesTheKeycloakIdentity() {
        // A missing row only means this account was never provisioned
        // app-side (e.g. Delete called before any GET /me) — it must not be
        // treated as "already deleted", or the Keycloak identity would never
        // actually get removed.
        UUID keycloakUserId = UUID.randomUUID();
        when(userRepositoryPort.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.empty());

        service.execute(keycloakUserId);

        verify(userRepositoryPort, never()).save(any());
        verify(identityProviderPort).deleteIdentity(keycloakUserId);
    }

    @Test
    void repeatCallOnAlreadyDeletedProfileIsNoOp() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.markDeleted();
        when(userRepositoryPort.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(profile));

        service.execute(keycloakUserId);

        verify(userRepositoryPort, never()).save(any());
        verify(identityProviderPort, never()).deleteIdentity(any());
    }
}
