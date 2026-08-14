package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetCurrentUserService implements GetCurrentUserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public GetCurrentUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public UserProfile execute(UUID keycloakUserId, String usernameHint) {
        return userRepositoryPort.findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> userRepositoryPort.save(UserProfile.provision(keycloakUserId, usernameHint)));
    }
}
