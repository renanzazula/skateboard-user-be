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

    private UserProfile(Snapshot snapshot) {
        this.id = snapshot.id();
        this.keycloakUserId = snapshot.keycloakUserId();
        this.username = snapshot.username();
        this.displayName = snapshot.displayName();
        this.profilePictureUrl = snapshot.profilePictureUrl();
        this.profilePictureObjectKey = snapshot.profilePictureObjectKey();
        this.status = snapshot.status();
        this.createdAt = snapshot.createdAt();
        this.updatedAt = snapshot.updatedAt();
    }

    /** Lazy-provisioning factory — used the first time a Keycloak-authenticated
     * caller is seen with no matching row yet (see GetCurrentUserService). */
    public static UserProfile provision(UUID keycloakUserId, String username) {
        Instant now = Instant.now();
        return new UserProfile(new Snapshot(UUID.randomUUID(), keycloakUserId, username, null, null, null,
                AccountStatus.ACTIVE, now, now));
    }

    public static UserProfile reconstitute(Snapshot snapshot) {
        return new UserProfile(snapshot);
    }

    /** Parameter object grouping every persisted field, so rehydrating from storage
     * (see UserPersistenceAdapter) doesn't require a wide constructor/factory. */
    public record Snapshot(UUID id, UUID keycloakUserId, String username, String displayName,
                            String profilePictureUrl, String profilePictureObjectKey,
                            AccountStatus status, Instant createdAt, Instant updatedAt) {
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
