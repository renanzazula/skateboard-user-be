package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.LookupUsersUseCase;
import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.domain.model.IdentitySummary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LookupUsersService implements LookupUsersUseCase {

    private final IdentityProviderPort identityProviderPort;

    public LookupUsersService(IdentityProviderPort identityProviderPort) {
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    public List<IdentitySummary> execute(List<UUID> keycloakUserIds) {
        return identityProviderPort.findIdentities(keycloakUserIds);
    }
}
