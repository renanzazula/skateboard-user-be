package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.UpdateNotificationPreferencesUseCase;
import com.skateboard.user.application.port.out.NotificationPreferencesRepositoryPort;
import com.skateboard.user.domain.model.NotificationPreferences;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UpdateNotificationPreferencesServiceTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Mock
    private NotificationPreferencesRepositoryPort preferencesRepositoryPort;

    private UpdateNotificationPreferencesService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UpdateNotificationPreferencesService(getCurrentUserUseCase, preferencesRepositoryPort);
    }

    @Test
    void omittedFieldLeavesExistingValueUnchanged() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        NotificationPreferences existing = NotificationPreferences.createDefault(profile.getId());
        existing.update(false, true); // pushEnabled=false, newPodcastEnabled=true

        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(preferencesRepositoryPort.findByUserId(profile.getId())).thenReturn(Optional.of(existing));
        when(preferencesRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferences updated = service.execute(
                new UpdateNotificationPreferencesUseCase.Input(keycloakUserId, null, false));

        assertThat(updated.isPushEnabled()).isFalse(); // unchanged
        assertThat(updated.isNewPodcastEnabled()).isFalse(); // changed
    }

    @Test
    void createsDefaultPreferencesWhenNoneExistYet() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(preferencesRepositoryPort.findByUserId(profile.getId())).thenReturn(Optional.empty());
        when(preferencesRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationPreferences updated = service.execute(
                new UpdateNotificationPreferencesUseCase.Input(keycloakUserId, false, null));

        assertThat(updated.isPushEnabled()).isFalse();
        assertThat(updated.isNewPodcastEnabled()).isTrue(); // default
    }
}
