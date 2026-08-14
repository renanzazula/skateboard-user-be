package com.skateboard.user.application.port.in;

import com.skateboard.user.domain.model.NotificationPreferences;

import java.util.UUID;

public interface GetNotificationPreferencesUseCase {
    NotificationPreferences execute(UUID keycloakUserId);
}
