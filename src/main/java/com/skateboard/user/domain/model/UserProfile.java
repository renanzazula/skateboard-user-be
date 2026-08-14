package com.skateboard.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public class UserProfile {

    private final UUID id;
    private final UUID keycloakUserId;
    private String username;
    private String displayName;
    private String profilePictureUrl;
    private String profilePictureObjectKey;
    private AccountStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserProfile(UUID id, UUID keycloakUserId, String username, String displayName,
                         String profilePictureUrl, String profilePictureObjectKey, AccountStatus status,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.displayName = displayName;
        this.profilePictureUrl = profilePictureUrl;
        this.profilePictureObjectKey = profilePictureObjectKey;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Lazy-provisioning factory — used the first time a Keycloak-authenticated
     * caller is seen with no matching row yet (see GetCurrentUserService). */
    public static UserProfile provision(UUID keycloakUserId, String username) {
        Instant now = Instant.now();
        return new UserProfile(UUID.randomUUID(), keycloakUserId, username, null, null, null,
                AccountStatus.ACTIVE, now, now);
    }

    public static UserProfile reconstitute(UUID id, UUID keycloakUserId, String username, String displayName,
                                            String profilePictureUrl, String profilePictureObjectKey,
                                            AccountStatus status, Instant createdAt, Instant updatedAt) {
        return new UserProfile(id, keycloakUserId, username, displayName, profilePictureUrl,
                profilePictureObjectKey, status, createdAt, updatedAt);
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
        this.updatedAt = Instant.now();
    }

    public void updateProfilePicture(String profilePictureUrl, String profilePictureObjectKey) {
        this.profilePictureUrl = profilePictureUrl;
        this.profilePictureObjectKey = profilePictureObjectKey;
        this.updatedAt = Instant.now();
    }

    public void changeUsername(String username) {
        this.username = username;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = AccountStatus.DEACTIVATED;
        this.updatedAt = Instant.now();
    }

    /** Anonymizes app-owned fields; the Keycloak identity is deleted separately
     * by the caller (see DeleteCurrentUserService). */
    public void markDeleted() {
        this.status = AccountStatus.DELETED;
        this.displayName = null;
        this.profilePictureUrl = null;
        this.profilePictureObjectKey = null;
        this.updatedAt = Instant.now();
    }

    public UUID getId()                          { return id; }
    public UUID getKeycloakUserId()               { return keycloakUserId; }
    public String getUsername()                   { return username; }
    public String getDisplayName()                { return displayName; }
    public String getProfilePictureUrl()          { return profilePictureUrl; }
    public String getProfilePictureObjectKey()    { return profilePictureObjectKey; }
    public AccountStatus getStatus()              { return status; }
    public Instant getCreatedAt()                 { return createdAt; }
    public Instant getUpdatedAt()                 { return updatedAt; }
}
