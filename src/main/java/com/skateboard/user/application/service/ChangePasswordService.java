package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.ChangePasswordUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import org.springframework.stereotype.Service;

/** Passwords are never stored or hashed here — this is a pure pass-through to Keycloak. */
@Service
public class ChangePasswordService implements ChangePasswordUseCase {

    private final IdentityProviderPort identityProviderPort;

    public ChangePasswordService(IdentityProviderPort identityProviderPort) {
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    public void execute(Input input) {
        identityProviderPort.resetPassword(input.keycloakUserId(), input.newPassword());
    }
}
