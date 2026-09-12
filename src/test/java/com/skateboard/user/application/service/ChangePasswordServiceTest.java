package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.ChangePasswordUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ChangePasswordServiceTest {

    @Mock
    private IdentityProviderPort identityProviderPort;

    private ChangePasswordService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ChangePasswordService(identityProviderPort);
    }

    @Test
    void passesThroughToTheIdentityProviderWithoutStoringOrHashingThePassword() {
        UUID keycloakUserId = UUID.randomUUID();

        service.execute(new ChangePasswordUseCase.Input(keycloakUserId, "s3cr3t-new-password"));

        verify(identityProviderPort).resetPassword(keycloakUserId, "s3cr3t-new-password");
        verifyNoMoreInteractions(identityProviderPort);
    }
}
