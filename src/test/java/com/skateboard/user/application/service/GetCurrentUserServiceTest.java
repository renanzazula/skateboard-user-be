package com.skateboard.user.application.service;

import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.AccountStatus;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetCurrentUserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    private GetCurrentUserService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GetCurrentUserService(userRepositoryPort);
    }

    @Test
    void returnsExistingProfileWithoutProvisioning() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile existing = UserProfile.provision(keycloakUserId, "rzazula");
        when(userRepositoryPort.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(existing));

        UserProfile result = service.execute(keycloakUserId, "ignored-hint");

        assertThat(result).isSameAs(existing);
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void provisionsNewProfileOnFirstAccess() {
        UUID keycloakUserId = UUID.randomUUID();
        when(userRepositoryPort.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.empty());
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.execute(keycloakUserId, "rzazula");

        assertThat(result.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(result.getUsername()).isEqualTo("rzazula");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepositoryPort).save(any());
    }
}
