package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.UploadProfilePictureUseCase;
import com.skateboard.user.application.port.out.ProfileImageStoragePort;
import com.skateboard.user.application.port.out.UserRepositoryPort;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadProfilePictureServiceTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private ProfileImageStoragePort profileImageStoragePort;

    private UploadProfilePictureService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UploadProfilePictureService(getCurrentUserUseCase, userRepositoryPort, profileImageStoragePort);
    }

    @Test
    void uploadsAndStoresTheNewPictureWhenNoPreviousOneExists() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        InputStream content = new ByteArrayInputStream("image-bytes".getBytes());
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(profileImageStoragePort.upload(eq(keycloakUserId), eq("avatar.png"), eq("image/png"), eq(content), eq(1024L)))
                .thenReturn(new ProfileImageStoragePort.StoredImage("objects/avatar.png", "https://storage/avatar.png"));
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.execute(new UploadProfilePictureUseCase.Input(
                keycloakUserId, "avatar.png", "image/png", content, 1024L));

        assertThat(result.getProfilePictureUrl()).isEqualTo("https://storage/avatar.png");
        assertThat(result.getProfilePictureObjectKey()).isEqualTo("objects/avatar.png");
        verify(profileImageStoragePort, never()).delete(anyString());
    }

    @Test
    void deletesThePreviousPictureObjectAfterSavingTheNewOne() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.updateProfilePicture("https://storage/old.png", "objects/old.png");
        InputStream content = new ByteArrayInputStream("image-bytes".getBytes());
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(profileImageStoragePort.upload(any(), anyString(), anyString(), any(), anyLong()))
                .thenReturn(new ProfileImageStoragePort.StoredImage("objects/new.png", "https://storage/new.png"));
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserProfile result = service.execute(new UploadProfilePictureUseCase.Input(
                keycloakUserId, "new.png", "image/png", content, 2048L));

        assertThat(result.getProfilePictureObjectKey()).isEqualTo("objects/new.png");
        ArgumentCaptor<String> deletedKey = ArgumentCaptor.forClass(String.class);
        verify(profileImageStoragePort).delete(deletedKey.capture());
        assertThat(deletedKey.getValue()).isEqualTo("objects/old.png");
    }

    @Test
    void aFailureDeletingThePreviousPictureIsSwallowedRatherThanFailingTheUpload() {
        // Deletion of the old object is best-effort cleanup: the new picture
        // has already been uploaded and saved by this point, so the caller
        // must still get back a successful result even if the old object
        // can't be removed from storage.
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        profile.updateProfilePicture("https://storage/old.png", "objects/old.png");
        InputStream content = new ByteArrayInputStream("image-bytes".getBytes());
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(profileImageStoragePort.upload(any(), anyString(), anyString(), any(), anyLong()))
                .thenReturn(new ProfileImageStoragePort.StoredImage("objects/new.png", "https://storage/new.png"));
        when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("storage unavailable"))
                .when(profileImageStoragePort).delete("objects/old.png");
        UploadProfilePictureUseCase.Input input = new UploadProfilePictureUseCase.Input(
                keycloakUserId, "new.png", "image/png", content, 2048L);

        // If the delete failure propagated, execute() would throw and this
        // assignment would never complete.
        UserProfile result = service.execute(input);

        assertThat(result.getProfilePictureObjectKey()).isEqualTo("objects/new.png");
        verify(profileImageStoragePort).delete("objects/old.png");
    }
}
