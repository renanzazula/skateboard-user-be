package com.skateboard.user.adapter.out.persistence;

import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.AccountStatus;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringUserProfileRepository jpaRepository;

    public UserPersistenceAdapter(SpringUserProfileRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserProfile> findByKeycloakUserId(UUID keycloakUserId) {
        return jpaRepository.findByKeycloakUserId(keycloakUserId).map(this::toDomain);
    }

    @Override
    public UserProfile save(UserProfile profile) {
        UserProfileJpaEntity entity = jpaRepository.findById(profile.getId()).orElseGet(UserProfileJpaEntity::new);
        toEntity(profile, entity);
        return toDomain(jpaRepository.save(entity));
    }

    private UserProfile toDomain(UserProfileJpaEntity e) {
        return UserProfile.reconstitute(
                e.getId(), e.getKeycloakUserId(), e.getUsername(), e.getDisplayName(),
                e.getProfilePictureUrl(), e.getProfilePictureObjectKey(),
                AccountStatus.valueOf(e.getAccountStatus()),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private void toEntity(UserProfile profile, UserProfileJpaEntity e) {
        e.setId(profile.getId());
        e.setKeycloakUserId(profile.getKeycloakUserId());
        e.setUsername(profile.getUsername());
        e.setDisplayName(profile.getDisplayName());
        e.setProfilePictureUrl(profile.getProfilePictureUrl());
        e.setProfilePictureObjectKey(profile.getProfilePictureObjectKey());
        e.setAccountStatus(profile.getStatus().name());
        e.setCreatedAt(profile.getCreatedAt());
        e.setUpdatedAt(profile.getUpdatedAt());
    }
}
