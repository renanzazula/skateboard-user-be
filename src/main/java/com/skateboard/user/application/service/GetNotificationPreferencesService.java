package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.GetNotificationPreferencesUseCase;
import com.skateboard.user.application.port.out.NotificationPreferencesRepositoryPort;
import com.skateboard.user.domain.model.NotificationPreferences;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetNotificationPreferencesService implements GetNotificationPreferencesUseCase {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final NotificationPreferencesRepositoryPort preferencesRepositoryPort;

    public GetNotificationPreferencesService(GetCurrentUserUseCase getCurrentUserUseCase,
                                              NotificationPreferencesRepositoryPort preferencesRepositoryPort) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.preferencesRepositoryPort = preferencesRepositoryPort;
    }

    @Override
    public NotificationPreferences execute(UUID keycloakUserId) {
        UserProfile profile = getCurrentUserUseCase.execute(keycloakUserId, null);
        return preferencesRepositoryPort.findByUserId(profile.getId())
                .orElseGet(() -> preferencesRepositoryPort.save(NotificationPreferences.createDefault(profile.getId())));
    }
}
