package com.skateboard.user.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences")
public class NotificationPreferencesJpaEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "new_podcast_enabled", nullable = false)
    private boolean newPodcastEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationPreferencesJpaEntity() {}

    public UUID getUserId()               { return userId; }
    public boolean isPushEnabled()        { return pushEnabled; }
    public boolean isNewPodcastEnabled()  { return newPodcastEnabled; }
    public Instant getUpdatedAt()         { return updatedAt; }

    public void setUserId(UUID userId)                       { this.userId = userId; }
    public void setPushEnabled(boolean pushEnabled)          { this.pushEnabled = pushEnabled; }
    public void setNewPodcastEnabled(boolean v)              { this.newPodcastEnabled = v; }
    public void setUpdatedAt(Instant updatedAt)              { this.updatedAt = updatedAt; }
}
