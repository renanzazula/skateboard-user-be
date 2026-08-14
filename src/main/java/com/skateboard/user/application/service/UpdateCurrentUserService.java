package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.UpdateCurrentUserUseCase;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class UpdateCurrentUserService implements UpdateCurrentUserUseCase {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UserRepositoryPort userRepositoryPort;

    public UpdateCurrentUserService(GetCurrentUserUseCase getCurrentUserUseCase, UserRepositoryPort userRepositoryPort) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public UserProfile execute(Input input) {
        // Fetch-or-provision rather than requiring GET /me to have run
        // first — every /me operation should work for any authenticated
        // caller regardless of call order.
        UserProfile profile = getCurrentUserUseCase.execute(input.keycloakUserId(), null);
        profile.updateDisplayName(input.displayName());
        return userRepositoryPort.save(profile);
    }
}
