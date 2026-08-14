package com.skateboard.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public class NotificationPreferences {

    private final UUID userId;
    private boolean pushEnabled;
    private boolean newPodcastEnabled;
    private Instant updatedAt;

    private NotificationPreferences(UUID userId, boolean pushEnabled, boolean newPodcastEnabled, Instant updatedAt) {
        this.userId = userId;
        this.pushEnabled = pushEnabled;
        this.newPodcastEnabled = newPodcastEnabled;
        this.updatedAt = updatedAt;
    }

    public static NotificationPreferences createDefault(UUID userId) {
        return new NotificationPreferences(userId, true, true, Instant.now());
    }

    public static NotificationPreferences reconstitute(UUID userId, boolean pushEnabled, boolean newPodcastEnabled,
                                                         Instant updatedAt) {
        return new NotificationPreferences(userId, pushEnabled, newPodcastEnabled, updatedAt);
    }

    /** Null fields are left unchanged — matches the README's partial-update example. */
    public void update(Boolean pushEnabled, Boolean newPodcastEnabled) {
        if (pushEnabled != null) this.pushEnabled = pushEnabled;
        if (newPodcastEnabled != null) this.newPodcastEnabled = newPodcastEnabled;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId()              { return userId; }
    public boolean isPushEnabled()       { return pushEnabled; }
    public boolean isNewPodcastEnabled() { return newPodcastEnabled; }
    public Instant getUpdatedAt()        { return updatedAt; }
}
