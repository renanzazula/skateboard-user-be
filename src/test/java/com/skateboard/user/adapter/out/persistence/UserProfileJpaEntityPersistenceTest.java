package com.skateboard.user.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the real Flyway-managed schema (V1__user_profile.sql) against an
 * actual Postgres instance, so the {@code @Column}/{@code @Table} mapping on
 * {@link UserProfileJpaEntity} is verified against production DDL rather than
 * an approximation (H2) of it.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserProfileJpaEntityPersistenceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private SpringUserProfileRepository repository;

    @Autowired
    private TestEntityManager entityManager;

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
        e.setUpdatedAt(Instant.parse("2024-01-02T11:30:00Z"));
        return e;
    }

    @Test
    void persistsAndReloadsEveryColumn() {
        UUID id = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        repository.saveAndFlush(fullEntity(id, keycloakUserId));
        entityManager.clear();

        UserProfileJpaEntity reloaded = repository.findById(id).orElseThrow();

        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(reloaded.getUsername()).isEqualTo("rzazula");
        assertThat(reloaded.getDisplayName()).isEqualTo("Renan Z");
        assertThat(reloaded.getProfilePictureUrl()).isEqualTo("https://storage.example/pic.png");
        assertThat(reloaded.getProfilePictureObjectKey()).isEqualTo("profile-pictures/" + id);
        assertThat(reloaded.getAccountStatus()).isEqualTo("ACTIVE");
        assertThat(reloaded.getCreatedAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
        assertThat(reloaded.getUpdatedAt()).isEqualTo(Instant.parse("2024-01-02T11:30:00Z"));
    }

    @Test
    void findByKeycloakUserId_locatesThePersistedRow() {
        UUID id = UUID.randomUUID();
        UUID keycloakUserId = UUID.randomUUID();
        repository.saveAndFlush(fullEntity(id, keycloakUserId));
        entityManager.clear();

        Optional<UserProfileJpaEntity> found = repository.findByKeycloakUserId(keycloakUserId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
    }

    @Test
    void findByKeycloakUserId_returnsEmpty_whenNoRowMatches() {
        Optional<UserProfileJpaEntity> found = repository.findByKeycloakUserId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void optionalColumns_canBeNull() {
        UserProfileJpaEntity minimal = new UserProfileJpaEntity();
        minimal.setId(UUID.randomUUID());
        minimal.setKeycloakUserId(UUID.randomUUID());
        minimal.setAccountStatus("ACTIVE");
        minimal.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        minimal.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        // username, displayName, profilePictureUrl, profilePictureObjectKey left null on purpose.

        repository.saveAndFlush(minimal);
        entityManager.clear();
        UserProfileJpaEntity reloaded = repository.findById(minimal.getId()).orElseThrow();

        assertThat(reloaded.getUsername()).isNull();
        assertThat(reloaded.getDisplayName()).isNull();
        assertThat(reloaded.getProfilePictureUrl()).isNull();
        assertThat(reloaded.getProfilePictureObjectKey()).isNull();
    }

    @Test
    void keycloakUserId_mustBeUnique_perRealSchemaConstraint() {
        UUID sharedKeycloakUserId = UUID.randomUUID();
        repository.saveAndFlush(fullEntity(UUID.randomUUID(), sharedKeycloakUserId));

        UserProfileJpaEntity duplicate = fullEntity(UUID.randomUUID(), sharedKeycloakUserId);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void accountStatus_cannotBeNull_perRealSchemaConstraint() {
        UserProfileJpaEntity entity = fullEntity(UUID.randomUUID(), UUID.randomUUID());
        entity.setAccountStatus(null);

        assertThatThrownBy(() -> repository.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
