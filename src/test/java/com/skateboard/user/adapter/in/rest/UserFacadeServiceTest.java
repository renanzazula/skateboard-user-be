package com.skateboard.user.adapter.in.rest;

import com.skateboard.application.dto.*;
import com.skateboard.user.application.port.in.*;
import com.skateboard.user.domain.model.NotificationPreferences;
import com.skateboard.user.domain.model.ProblemReport;
import com.skateboard.user.domain.model.ProblemReportCategory;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserFacadeServiceTest {

    @Mock private GetCurrentUserUseCase getCurrentUserUseCase;
    @Mock private UpdateCurrentUserUseCase updateCurrentUserUseCase;
    @Mock private GetNotificationPreferencesUseCase getNotificationPreferencesUseCase;
    @Mock private UpdateNotificationPreferencesUseCase updateNotificationPreferencesUseCase;
    @Mock private UploadProfilePictureUseCase uploadProfilePictureUseCase;
    @Mock private ChangeUsernameUseCase changeUsernameUseCase;
    @Mock private ChangePasswordUseCase changePasswordUseCase;
    @Mock private DeactivateCurrentUserUseCase deactivateCurrentUserUseCase;
    @Mock private DeleteCurrentUserUseCase deleteCurrentUserUseCase;
    @Mock private ReportProblemUseCase reportProblemUseCase;

    private UserFacadeService facade;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        facade = new UserFacadeService(getCurrentUserUseCase, updateCurrentUserUseCase,
                getNotificationPreferencesUseCase, updateNotificationPreferencesUseCase,
                uploadProfilePictureUseCase, changeUsernameUseCase, changePasswordUseCase,
                deactivateCurrentUserUseCase, deleteCurrentUserUseCase, reportProblemUseCase);
    }

    @Test
    void getCurrentUserMapsDomainToDto() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.updateDisplayName("Rzazula");
        when(getCurrentUserUseCase.execute(keycloakUserId, "rzazula")).thenReturn(profile);

        UserResponse response = facade.getCurrentUser(keycloakUserId, "rzazula");

        assertThat(response.getId()).isEqualTo(profile.getId());
        assertThat(response.getUsername()).isEqualTo("rzazula");
        assertThat(response.getDisplayName()).isEqualTo("Rzazula");
        assertThat(response.getStatus()).isEqualTo(com.skateboard.application.dto.AccountStatus.ACTIVE);
    }

    @Test
    void updateNotificationPreferencesUnwrapsNestedNotificationsObject() {
        UUID keycloakUserId = UUID.randomUUID();
        NotificationPreferences preferences = NotificationPreferences.createDefault(UUID.randomUUID());
        preferences.update(false, null);
        when(updateNotificationPreferencesUseCase.execute(any())).thenReturn(preferences);

        UpdateNotificationPreferencesRequest req = new UpdateNotificationPreferencesRequest()
                .notifications(new com.skateboard.application.dto.NotificationPreferences().pushEnabled(false));

        facade.updateNotificationPreferences(keycloakUserId, req);

        ArgumentCaptor<UpdateNotificationPreferencesUseCase.Input> captor =
                ArgumentCaptor.forClass(UpdateNotificationPreferencesUseCase.Input.class);
        verify(updateNotificationPreferencesUseCase).execute(captor.capture());
        assertThat(captor.getValue().pushEnabled()).isFalse();
        assertThat(captor.getValue().newPodcastEnabled()).isNull();
    }

    @Test
    void reportProblemMapsEnumsBothWays() {
        UUID keycloakUserId = UUID.randomUUID();
        ProblemReport report = ProblemReport.create(UUID.randomUUID(), ProblemReportCategory.APP_ERROR,
                "Unable to update profile picture", "1.4.0", com.skateboard.user.domain.model.ProblemReportPlatform.ANDROID);
        when(reportProblemUseCase.execute(any())).thenReturn(report);

        ProblemReportRequest req = new ProblemReportRequest()
                .category(com.skateboard.application.dto.ProblemReportCategory.APP_ERROR)
                .message("Unable to update profile picture")
                .appVersion("1.4.0")
                .platform(com.skateboard.application.dto.ProblemReportPlatform.ANDROID);

        ProblemReportResponse response = facade.reportProblem(keycloakUserId, req);

        assertThat(response.getCategory()).isEqualTo(com.skateboard.application.dto.ProblemReportCategory.APP_ERROR);
        assertThat(response.getPlatform()).isEqualTo(com.skateboard.application.dto.ProblemReportPlatform.ANDROID);
        assertThat(response.getMessage()).isEqualTo("Unable to update profile picture");
    }
}
