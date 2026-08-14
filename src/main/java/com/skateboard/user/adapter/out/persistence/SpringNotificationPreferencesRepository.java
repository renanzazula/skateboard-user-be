package com.skateboard.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringNotificationPreferencesRepository extends JpaRepository<NotificationPreferencesJpaEntity, UUID> {
}
