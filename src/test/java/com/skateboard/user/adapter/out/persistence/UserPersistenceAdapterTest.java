package com.skateboard.user.adapter.out.persistence;

import com.skateboard.user.domain.model.AccountStatus;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPersistenceAdapterTest {

    @Mock
    private SpringUserProfileRepository jpaRepository;

    private UserPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new UserPersistenceAdapter(jpaRepository);
    }

    private UserProfileJpaEntity fullEntity(UUID id, UUID keycloakUserId) {
        UserProfileJpaEntity e = new UserProfileJpaEntity();
        e.setId(id);
        e.setKeycloakUserId(keycloakUserId);
        e.setUsername("rzazula");
        e.setDisplayName("Renan Z");
        e.setProfilePictureUrl("https://storage.example/pic.png");
        e.setProfilePictureObjectKey("profile-pictures/" + id);
        e.setAccountStatus("ACTIVE");
        e.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));
        e.setUpdatedAt(Instant.parse("2024-01-02T10:00:00Z"));
        return e;
    }

    @Test
    void findByKeycloakUserId_mapsEveryFieldToDomain_whenEntityExists() {
        UUID id = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        UserProfileJpaEntity entity = fullEntity(id, keycloakUserId);
        when(jpaRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(entity));

        Optional<UserProfile> result = adapter.findByKeycloakUserId(keycloakUserId);

        assertThat(result).isPresent();
        UserProfile profile = result.get();
        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(profile.getUsername()).isEqualTo("rzazula");
        assertThat(profile.getDisplayName()).isEqualTo("Renan Z");
        assertThat(profile.getProfilePictureUrl()).isEqualTo("https://storage.example/pic.png");
        assertThat(profile.getProfilePictureObjectKey()).isEqualTo("profile-pictures/" + id);
        assertThat(profile.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(profile.getCreatedAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
        assertThat(profile.getUpdatedAt()).isEqualTo(Instant.parse("2024-01-02T10:00:00Z"));
        verify(jpaRepository).findByKeycloakUserId(keycloakUserId);
    }

    @Test
    void findByKeycloakUserId_returnsEmpty_whenRepositoryFindsNothing() {
        UUID keycloakUserId = UUID.randomUUID();
        when(jpaRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.empty());

        Optional<UserProfile> result = adapter.findByKeycloakUserId(keycloakUserId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByKeycloakUserId_throws_whenStoredAccountStatusIsNotAValidEnumValue() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfileJpaEntity entity = fullEntity(UUID.randomUUID(), keycloakUserId);
        entity.setAccountStatus("NOT_A_REAL_STATUS");
        when(jpaRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> adapter.findByKeycloakUserId(keycloakUserId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void save_createsNewEntity_whenNoRowExistsYetForProfileId() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "newuser");
        when(jpaRepository.findById(profile.getId())).thenReturn(Optional.empty());
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = adapter.save(profile);

        ArgumentCaptor<UserProfileJpaEntity> captor = ArgumentCaptor.forClass(UserProfileJpaEntity.class);
        verify(jpaRepository).save(captor.capture());
        UserProfileJpaEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(profile.getId());
        assertThat(saved.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getDisplayName()).isNull();
        assertThat(saved.getProfilePictureUrl()).isNull();
        assertThat(saved.getProfilePictureObjectKey()).isNull();
        assertThat(saved.getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getCreatedAt()).isEqualTo(profile.getCreatedAt());
        assertThat(saved.getUpdatedAt()).isEqualTo(profile.getUpdatedAt());

        assertThat(result.getId()).isEqualTo(profile.getId());
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void save_reusesAndMutatesExistingEntityInstance_whenRowAlreadyExistsForProfileId() {
        UUID keycloakUserId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        UserProfileJpaEntity existingEntity = fullEntity(id, keycloakUserId);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        UserProfile.Snapshot snapshot = new UserProfile.Snapshot(id, keycloakUserId, "renamed", "New Display Name",
                "https://storage.example/new.png", "profile-pictures/new", AccountStatus.DEACTIVATED,
                Instant.parse("2024-01-01T10:00:00Z"), now);
        UserProfile updatedProfile = UserProfile.reconstitute(snapshot);

        when(jpaRepository.findById(id)).thenReturn(Optional.of(existingEntity));
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = adapter.save(updatedProfile);

        ArgumentCaptor<UserProfileJpaEntity> captor = ArgumentCaptor.forClass(UserProfileJpaEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existingEntity);
        assertThat(existingEntity.getUsername()).isEqualTo("renamed");
        assertThat(existingEntity.getDisplayName()).isEqualTo("New Display Name");
        assertThat(existingEntity.getProfilePictureUrl()).isEqualTo("https://storage.example/new.png");
        assertThat(existingEntity.getProfilePictureObjectKey()).isEqualTo("profile-pictures/new");
        assertThat(existingEntity.getAccountStatus()).isEqualTo("DEACTIVATED");
        assertThat(existingEntity.getUpdatedAt()).isEqualTo(now);

        assertThat(result.getStatus()).isEqualTo(AccountStatus.DEACTIVATED);
        assertThat(result.getDisplayName()).isEqualTo("New Display Name");
        verify(jpaRepository, never()).findByKeycloakUserId(any());
    }
}
