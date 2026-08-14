package com.skateboard.user.adapter.out.persistence;

import com.skateboard.user.application.port.out.NotificationPreferencesRepositoryPort;
import com.skateboard.user.domain.model.NotificationPreferences;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationPreferencesPersistenceAdapter implements NotificationPreferencesRepositoryPort {

    private final SpringNotificationPreferencesRepository jpaRepository;

    public NotificationPreferencesPersistenceAdapter(SpringNotificationPreferencesRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<NotificationPreferences> findByUserId(UUID userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public NotificationPreferences save(NotificationPreferences preferences) {
        NotificationPreferencesJpaEntity entity = jpaRepository.findById(preferences.getUserId())
                .orElseGet(NotificationPreferencesJpaEntity::new);
        entity.setUserId(preferences.getUserId());
        entity.setPushEnabled(preferences.isPushEnabled());
        entity.setNewPodcastEnabled(preferences.isNewPodcastEnabled());
        entity.setUpdatedAt(preferences.getUpdatedAt());
        return toDomain(jpaRepository.save(entity));
    }

    private NotificationPreferences toDomain(NotificationPreferencesJpaEntity e) {
        return NotificationPreferences.reconstitute(
                e.getUserId(), e.isPushEnabled(), e.isNewPodcastEnabled(), e.getUpdatedAt());
    }
}
