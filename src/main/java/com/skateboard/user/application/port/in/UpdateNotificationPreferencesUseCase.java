package com.skateboard.user.application.port.in;

import com.skateboard.user.domain.model.NotificationPreferences;

import java.util.UUID;

public interface UpdateNotificationPreferencesUseCase {

    NotificationPreferences execute(Input input);

    /** Null fields mean "leave as-is" — matches the README's partial-update example. */
    record Input(UUID keycloakUserId, Boolean pushEnabled, Boolean newPodcastEnabled) {}
}
