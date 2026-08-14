package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.ChangeUsernameUseCase;
import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

class ChangeUsernameServiceTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private IdentityProviderPort identityProviderPort;

    private ChangeUsernameService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ChangeUsernameService(getCurrentUserUseCase, userRepositoryPort, identityProviderPort);
    }

    @Test
    void changesKeycloakUsernameBeforePersistingLocalCopy() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "old-name");
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile updated = service.execute(new ChangeUsernameUseCase.Input(keycloakUserId, "new-name"));

        assertThat(updated.getUsername()).isEqualTo("new-name");
        InOrder order = inOrder(identityProviderPort, userRepositoryPort);
        order.verify(identityProviderPort).changeUsername(keycloakUserId, "new-name");
        order.verify(userRepositoryPort).save(any());
    }

    @Test
    void delegatesToGetCurrentUserUseCaseSoAMissingProfileIsLazilyProvisionedRatherThanRejected() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile provisioned = UserProfile.provision(keycloakUserId, null);
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(provisioned);
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile updated = service.execute(new ChangeUsernameUseCase.Input(keycloakUserId, "new-name"));

        assertThat(updated.getUsername()).isEqualTo("new-name");
    }
}
