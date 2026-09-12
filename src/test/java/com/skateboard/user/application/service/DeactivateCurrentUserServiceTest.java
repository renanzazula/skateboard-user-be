package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.AccountStatus;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeactivateCurrentUserServiceTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private IdentityProviderPort identityProviderPort;

    private DeactivateCurrentUserService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DeactivateCurrentUserService(getCurrentUserUseCase, userRepositoryPort, identityProviderPort);
    }

    @Test
    void deactivatesProfileSavesItAndThenDisablesTheKeycloakIdentity() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.execute(keycloakUserId);

        assertThat(result.getStatus()).isEqualTo(AccountStatus.DEACTIVATED);
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.DEACTIVATED);

        // The local status change must be persisted before the Keycloak
        // identity is disabled/session-revoked, so a failure disabling the
        // identity never leaves the account looking ACTIVE locally.
        InOrder order = inOrder(userRepositoryPort, identityProviderPort);
        order.verify(userRepositoryPort).save(any());
        order.verify(identityProviderPort).disableIdentity(keycloakUserId);
    }

    @Test
    void returnsTheSavedInstanceRatherThanTheOriginalProfileReference() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        UserProfile savedProfile = UserProfile.provision(keycloakUserId, "rzazula");
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(userRepositoryPort.save(any())).thenReturn(savedProfile);

        UserProfile result = service.execute(keycloakUserId);

        assertThat(result).isSameAs(savedProfile);
    }
}
