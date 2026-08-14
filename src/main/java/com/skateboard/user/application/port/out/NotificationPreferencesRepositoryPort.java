package com.skateboard.user.application.port.out;

import com.skateboard.user.domain.model.NotificationPreferences;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesRepositoryPort {
    Optional<NotificationPreferences> findByUserId(UUID userId);
    NotificationPreferences save(NotificationPreferences preferences);
}
