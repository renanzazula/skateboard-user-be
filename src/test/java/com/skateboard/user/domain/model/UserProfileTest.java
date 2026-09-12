package com.skateboard.user.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileTest {

    @Test
    void provisionCreatesAnActiveProfileWithARandomIdAndTheGivenUsername() {
        UUID keycloakUserId = UUID.randomUUID();

        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");

        assertThat(profile.getId()).isNotNull();
        assertThat(profile.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(profile.getUsername()).isEqualTo("rzazula");
        assertThat(profile.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(profile.getDisplayName()).isNull();
        assertThat(profile.getProfilePictureUrl()).isNull();
        assertThat(profile.getProfilePictureObjectKey()).isNull();
        assertThat(profile.getCreatedAt()).isNotNull();
        assertThat(profile.getUpdatedAt()).isEqualTo(profile.getCreatedAt());
    }

    @Test
    void reconstituteRehydratesEveryFieldFromAPersistedSnapshotUnchanged() {
        UUID id = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2024-02-01T00:00:00Z");
        UserProfile.Snapshot snapshot = new UserProfile.Snapshot(id, keycloakUserId, "rzazula", "Renan Zazula",
                "https://storage/pic.png", "objects/pic.png", AccountStatus.DEACTIVATED, createdAt, updatedAt);

        UserProfile profile = UserProfile.reconstitute(snapshot);

        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(profile.getUsername()).isEqualTo("rzazula");
        assertThat(profile.getDisplayName()).isEqualTo("Renan Zazula");
        assertThat(profile.getProfilePictureUrl()).isEqualTo("https://storage/pic.png");
        assertThat(profile.getProfilePictureObjectKey()).isEqualTo("objects/pic.png");
        assertThat(profile.getStatus()).isEqualTo(AccountStatus.DEACTIVATED);
        assertThat(profile.getCreatedAt()).isEqualTo(createdAt);
        assertThat(profile.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void updateDisplayNameChangesOnlyTheDisplayNameAndBumpsUpdatedAt() {
        UserProfile profile = UserProfile.provision(UUID.randomUUID(), "rzazula");
        Instant originalUpdatedAt = profile.getUpdatedAt();

        profile.updateDisplayName("Renan Zazula");

        assertThat(profile.getDisplayName()).isEqualTo("Renan Zazula");
        assertThat(profile.getUsername()).isEqualTo("rzazula");
        assertThat(profile.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void updateProfilePictureSetsBothTheUrlAndTheObjectKey() {
        UserProfile profile = UserProfile.provision(UUID.randomUUID(), "rzazula");

        profile.updateProfilePicture("https://storage/new.png", "objects/new.png");

        assertThat(profile.getProfilePictureUrl()).isEqualTo("https://storage/new.png");
        assertThat(profile.getProfilePictureObjectKey()).isEqualTo("objects/new.png");
    }

    @Test
    void changeUsernameUpdatesTheUsernameOnly() {
        UserProfile profile = UserProfile.provision(UUID.randomUUID(), "old-name");

        profile.changeUsername("new-name");

        assertThat(profile.getUsername()).isEqualTo("new-name");
    }

    @Test
    void deactivateSetsStatusToDeactivatedWithoutTouchingOtherFields() {
        UserProfile profile = UserProfile.provision(UUID.randomUUID(), "rzazula");
        profile.updateDisplayName("Renan Zazula");

        profile.deactivate();

        assertThat(profile.getStatus()).isEqualTo(AccountStatus.DEACTIVATED);
        assertThat(profile.getDisplayName()).isEqualTo("Renan Zazula");
    }

    @Test
    void markDeletedAnonymizesAppOwnedFieldsButKeepsIdentityAndTimestamps() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.updateDisplayName("Renan Zazula");
        profile.updateProfilePicture("https://storage/pic.png", "objects/pic.png");
        UUID id = profile.getId();
        Instant createdAt = profile.getCreatedAt();

        profile.markDeleted();

        assertThat(profile.getStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(profile.getDisplayName()).isNull();
        assertThat(profile.getProfilePictureUrl()).isNull();
        assertThat(profile.getProfilePictureObjectKey()).isNull();
        // The row itself — id, keycloak identity link, creation time — is
        // kept; this is anonymize-in-place, not a row delete.
        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(profile.getCreatedAt()).isEqualTo(createdAt);
    }
}
