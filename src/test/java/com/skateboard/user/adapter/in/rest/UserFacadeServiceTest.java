package com.skateboard.user.adapter.in.rest;

import com.skateboard.application.dto.*;
import com.skateboard.user.application.port.in.*;
import com.skateboard.user.application.port.out.ProfileImageStoragePort;
import com.skateboard.user.domain.model.ProblemReport;
import com.skateboard.user.domain.model.ProblemReportCategory;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserFacadeServiceTest {

    @Mock private GetCurrentUserUseCase getCurrentUserUseCase;
    @Mock private UpdateCurrentUserUseCase updateCurrentUserUseCase;
    @Mock private UploadProfilePictureUseCase uploadProfilePictureUseCase;
    @Mock private ChangeUsernameUseCase changeUsernameUseCase;
    @Mock private ChangePasswordUseCase changePasswordUseCase;
    @Mock private DeactivateCurrentUserUseCase deactivateCurrentUserUseCase;
    @Mock private DeleteCurrentUserUseCase deleteCurrentUserUseCase;
    @Mock private ReportProblemUseCase reportProblemUseCase;
    @Mock private ProfileImageStoragePort profileImageStoragePort;

    private UserFacadeService facade;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        facade = new UserFacadeService(getCurrentUserUseCase, updateCurrentUserUseCase,
                uploadProfilePictureUseCase, changeUsernameUseCase, changePasswordUseCase,
                deactivateCurrentUserUseCase, deleteCurrentUserUseCase, reportProblemUseCase,
                profileImageStoragePort);
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

    @Test
    void updateCurrentUserPassesDisplayNameToUseCaseAndMapsResult() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.updateDisplayName("Renan Z");
        UpdateUserRequest req = new UpdateUserRequest().displayName("Renan Z");
        ArgumentCaptor<UpdateCurrentUserUseCase.Input> captor = ArgumentCaptor.forClass(UpdateCurrentUserUseCase.Input.class);
        when(updateCurrentUserUseCase.execute(any())).thenReturn(profile);

        UserResponse response = facade.updateCurrentUser(keycloakUserId, req);

        verify(updateCurrentUserUseCase).execute(captor.capture());
        assertThat(captor.getValue().keycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(captor.getValue().displayName()).isEqualTo("Renan Z");
        assertThat(response.getDisplayName()).isEqualTo("Renan Z");
    }

    @Test
    void uploadProfilePictureMapsFileMetadataAndDomainResult() throws IOException {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.updateProfilePicture("https://ignored", "objects/rzazula/avatar.png");
        MultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
        ArgumentCaptor<UploadProfilePictureUseCase.Input> captor =
                ArgumentCaptor.forClass(UploadProfilePictureUseCase.Input.class);
        when(uploadProfilePictureUseCase.execute(any())).thenReturn(profile);
        when(profileImageStoragePort.presignGetUrl("objects/rzazula/avatar.png")).thenReturn("https://presigned/avatar.png");

        UserResponse response = facade.uploadProfilePicture(keycloakUserId, file);

        verify(uploadProfilePictureUseCase).execute(captor.capture());
        UploadProfilePictureUseCase.Input input = captor.getValue();
        assertThat(input.keycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(input.filename()).isEqualTo("avatar.png");
        assertThat(input.contentType()).isEqualTo("image/png");
        assertThat(input.contentLength()).isEqualTo(3L);
        assertThat(response.getProfilePictureUrl()).isEqualTo("https://presigned/avatar.png");
    }

    @Test
    void uploadProfilePictureWrapsIOExceptionAsUncheckedIOException() throws IOException {
        UUID keycloakUserId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> facade.uploadProfilePicture(keycloakUserId, file))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(IOException.class);
        verify(uploadProfilePictureUseCase, never()).execute(any());
    }

    @Test
    void changeUsernameMapsRequestAndDomainResult() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.changeUsername("newname");
        ChangeUsernameRequest req = new ChangeUsernameRequest().username("newname");
        ArgumentCaptor<ChangeUsernameUseCase.Input> captor = ArgumentCaptor.forClass(ChangeUsernameUseCase.Input.class);
        when(changeUsernameUseCase.execute(any())).thenReturn(profile);

        UserResponse response = facade.changeUsername(keycloakUserId, req);

        verify(changeUsernameUseCase).execute(captor.capture());
        assertThat(captor.getValue().keycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(captor.getValue().newUsername()).isEqualTo("newname");
        assertThat(response.getUsername()).isEqualTo("newname");
    }

    @Test
    void changePasswordDelegatesToUseCaseWithNewPassword() {
        UUID keycloakUserId = UUID.randomUUID();
        ChangePasswordRequest req = new ChangePasswordRequest().newPassword("s3cret-password");
        ArgumentCaptor<ChangePasswordUseCase.Input> captor = ArgumentCaptor.forClass(ChangePasswordUseCase.Input.class);

        facade.changePassword(keycloakUserId, req);

        verify(changePasswordUseCase).execute(captor.capture());
        assertThat(captor.getValue().keycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(captor.getValue().newPassword()).isEqualTo("s3cret-password");
    }

    @Test
    void deactivateCurrentUserMapsDomainToDto() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.deactivate();
        when(deactivateCurrentUserUseCase.execute(keycloakUserId)).thenReturn(profile);

        UserResponse response = facade.deactivateCurrentUser(keycloakUserId);

        assertThat(response.getStatus()).isEqualTo(com.skateboard.application.dto.AccountStatus.DEACTIVATED);
        verify(deactivateCurrentUserUseCase).execute(keycloakUserId);
    }

    @Test
    void deleteCurrentUserDelegatesToUseCase() {
        UUID keycloakUserId = UUID.randomUUID();

        facade.deleteCurrentUser(keycloakUserId);

        verify(deleteCurrentUserUseCase).execute(keycloakUserId);
    }

    @Test
    void reportProblemMapsNullPlatformToNull() {
        UUID keycloakUserId = UUID.randomUUID();
        ProblemReport report = ProblemReport.create(UUID.randomUUID(), ProblemReportCategory.OTHER,
                "General feedback", null, null);
        when(reportProblemUseCase.execute(any())).thenReturn(report);

        ProblemReportRequest req = new ProblemReportRequest()
                .category(com.skateboard.application.dto.ProblemReportCategory.OTHER)
                .message("General feedback");

        ProblemReportResponse response = facade.reportProblem(keycloakUserId, req);

        assertThat(response.getPlatform()).isNull();
        assertThat(response.getAppVersion()).isNull();
    }
}
