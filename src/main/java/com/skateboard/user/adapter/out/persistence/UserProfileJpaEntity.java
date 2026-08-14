package com.skateboard.user.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
public class UserProfileJpaEntity {

    @Id
    private UUID id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private UUID keycloakUserId;

    @Column
    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "profile_picture_url", columnDefinition = "text")
    private String profilePictureUrl;

    @Column(name = "profile_picture_object_key", columnDefinition = "text")
    private String profilePictureObjectKey;

    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserProfileJpaEntity() {}

    public UUID getId()                            { return id; }
    public UUID getKeycloakUserId()                { return keycloakUserId; }
    public String getUsername()                    { return username; }
    public String getDisplayName()                 { return displayName; }
    public String getProfilePictureUrl()           { return profilePictureUrl; }
    public String getProfilePictureObjectKey()     { return profilePictureObjectKey; }
    public String getAccountStatus()               { return accountStatus; }
    public Instant getCreatedAt()                  { return createdAt; }
    public Instant getUpdatedAt()                  { return updatedAt; }

    public void setId(UUID id)                                     { this.id = id; }
    public void setKeycloakUserId(UUID keycloakUserId)              { this.keycloakUserId = keycloakUserId; }
    public void setUsername(String username)                       { this.username = username; }
    public void setDisplayName(String displayName)                 { this.displayName = displayName; }
    public void setProfilePictureUrl(String profilePictureUrl)     { this.profilePictureUrl = profilePictureUrl; }
    public void setProfilePictureObjectKey(String v)               { this.profilePictureObjectKey = v; }
    public void setAccountStatus(String accountStatus)             { this.accountStatus = accountStatus; }
    public void setCreatedAt(Instant createdAt)                    { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt)                    { this.updatedAt = updatedAt; }
}
