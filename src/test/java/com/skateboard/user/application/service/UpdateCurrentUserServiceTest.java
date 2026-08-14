package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.UpdateCurrentUserUseCase;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UpdateCurrentUserServiceTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    private UpdateCurrentUserService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UpdateCurrentUserService(getCurrentUserUseCase, userRepositoryPort);
    }

    @Test
    void updatesDisplayNameOfExistingProfile() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile existing = UserProfile.provision(keycloakUserId, "rzazula");
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(existing);
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile updated = service.execute(new UpdateCurrentUserUseCase.Input(keycloakUserId, "New Name"));

        assertThat(updated.getDisplayName()).isEqualTo("New Name");
    }

    @Test
    void delegatesToGetCurrentUserUseCaseSoAMissingProfileIsLazilyProvisionedRatherThanRejected() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile provisioned = UserProfile.provision(keycloakUserId, null);
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(provisioned);
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile updated = service.execute(new UpdateCurrentUserUseCase.Input(keycloakUserId, "New Name"));

        assertThat(updated.getDisplayName()).isEqualTo("New Name");
    }
}
