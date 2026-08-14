package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.UpdateNotificationPreferencesUseCase;
import com.skateboard.user.application.port.out.NotificationPreferencesRepositoryPort;
import com.skateboard.user.domain.model.NotificationPreferences;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class UpdateNotificationPreferencesService implements UpdateNotificationPreferencesUseCase {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final NotificationPreferencesRepositoryPort preferencesRepositoryPort;

    public UpdateNotificationPreferencesService(GetCurrentUserUseCase getCurrentUserUseCase,
                                                 NotificationPreferencesRepositoryPort preferencesRepositoryPort) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.preferencesRepositoryPort = preferencesRepositoryPort;
    }

    @Override
    public NotificationPreferences execute(Input input) {
        UserProfile profile = getCurrentUserUseCase.execute(input.keycloakUserId(), null);
        NotificationPreferences preferences = preferencesRepositoryPort.findByUserId(profile.getId())
                .orElseGet(() -> NotificationPreferences.createDefault(profile.getId()));
        preferences.update(input.pushEnabled(), input.newPodcastEnabled());
        return preferencesRepositoryPort.save(preferences);
    }
}
