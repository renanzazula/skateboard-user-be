package com.skateboard.user.adapter.in.rest;

import com.skateboard.application.dto.*;
import com.skateboard.user.application.port.in.*;
import com.skateboard.user.domain.model.ProblemReport;
import com.skateboard.user.domain.model.UserProfile;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Maps between generated DTOs and the domain, and orchestrates use cases for
 * UserController. No caching here (unlike skateboard-podcast-be's
 * PodcastService): /me is single-row, per-caller, and mutated by the same
 * user who reads it, so caching would only risk serving stale data after a
 * write for no real benefit — the podcast feed's shared/read-heavy profile
 * doesn't apply here.
 */
@Service
public class UserFacadeService {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateCurrentUserUseCase updateCurrentUserUseCase;
    private final GetNotificationPreferencesUseCase getNotificationPreferencesUseCase;
    private final UpdateNotificationPreferencesUseCase updateNotificationPreferencesUseCase;
    private final UploadProfilePictureUseCase uploadProfilePictureUseCase;
    private final ChangeUsernameUseCase changeUsernameUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final DeactivateCurrentUserUseCase deactivateCurrentUserUseCase;
    private final DeleteCurrentUserUseCase deleteCurrentUserUseCase;
    private final ReportProblemUseCase reportProblemUseCase;

    public UserFacadeService(GetCurrentUserUseCase getCurrentUserUseCase,
                              UpdateCurrentUserUseCase updateCurrentUserUseCase,
                              GetNotificationPreferencesUseCase getNotificationPreferencesUseCase,
                              UpdateNotificationPreferencesUseCase updateNotificationPreferencesUseCase,
                              UploadProfilePictureUseCase uploadProfilePictureUseCase,
                              ChangeUsernameUseCase changeUsernameUseCase,
                              ChangePasswordUseCase changePasswordUseCase,
                              DeactivateCurrentUserUseCase deactivateCurrentUserUseCase,
                              DeleteCurrentUserUseCase deleteCurrentUserUseCase,
                              ReportProblemUseCase reportProblemUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.updateCurrentUserUseCase = updateCurrentUserUseCase;
        this.getNotificationPreferencesUseCase = getNotificationPreferencesUseCase;
        this.updateNotificationPreferencesUseCase = updateNotificationPreferencesUseCase;
        this.uploadProfilePictureUseCase = uploadProfilePictureUseCase;
        this.changeUsernameUseCase = changeUsernameUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.deactivateCurrentUserUseCase = deactivateCurrentUserUseCase;
        this.deleteCurrentUserUseCase = deleteCurrentUserUseCase;
        this.reportProblemUseCase = reportProblemUseCase;
    }

    public UserResponse getCurrentUser(UUID keycloakUserId, String usernameHint) {
        return toUserResponse(getCurrentUserUseCase.execute(keycloakUserId, usernameHint));
    }

    public UserResponse updateCurrentUser(UUID keycloakUserId, UpdateUserRequest req) {
        UserProfile profile = updateCurrentUserUseCase.execute(
                new UpdateCurrentUserUseCase.Input(keycloakUserId, req.getDisplayName()));
        return toUserResponse(profile);
    }

    public NotificationPreferencesResponse getNotificationPreferences(UUID keycloakUserId) {
        return toPreferencesResponse(getNotificationPreferencesUseCase.execute(keycloakUserId));
    }

    public NotificationPreferencesResponse updateNotificationPreferences(UUID keycloakUserId,
                                                                          UpdateNotificationPreferencesRequest req) {
        NotificationPreferences notifications = req.getNotifications();
        var preferences = updateNotificationPreferencesUseCase.execute(new UpdateNotificationPreferencesUseCase.Input(
                keycloakUserId,
                notifications != null ? notifications.getPushEnabled() : null,
                notifications != null ? notifications.getNewPodcastEnabled() : null));
        return toPreferencesResponse(preferences);
    }

    public UserResponse uploadProfilePicture(UUID keycloakUserId, MultipartFile file) {
        try {
            UserProfile profile = uploadProfilePictureUseCase.execute(new UploadProfilePictureUseCase.Input(
                    keycloakUserId, file.getOriginalFilename(), file.getContentType(),
                    file.getInputStream(), file.getSize()));
            return toUserResponse(profile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public UserResponse changeUsername(UUID keycloakUserId, ChangeUsernameRequest req) {
        UserProfile profile = changeUsernameUseCase.execute(
                new ChangeUsernameUseCase.Input(keycloakUserId, req.getUsername()));
        return toUserResponse(profile);
    }

    public void changePassword(UUID keycloakUserId, ChangePasswordRequest req) {
        changePasswordUseCase.execute(new ChangePasswordUseCase.Input(keycloakUserId, req.getNewPassword()));
    }

    public UserResponse deactivateCurrentUser(UUID keycloakUserId) {
        return toUserResponse(deactivateCurrentUserUseCase.execute(keycloakUserId));
    }

    public void deleteCurrentUser(UUID keycloakUserId) {
        deleteCurrentUserUseCase.execute(keycloakUserId);
    }

    public ProblemReportResponse reportProblem(UUID keycloakUserId, ProblemReportRequest req) {
        ProblemReport report = reportProblemUseCase.execute(new ReportProblemUseCase.Input(
                keycloakUserId,
                com.skateboard.user.domain.model.ProblemReportCategory.valueOf(req.getCategory().getValue()),
                req.getMessage(), req.getAppVersion(),
                req.getPlatform() != null
                        ? com.skateboard.user.domain.model.ProblemReportPlatform.valueOf(req.getPlatform().getValue())
                        : null));
        return toProblemReportResponse(report);
    }

    private UserResponse toUserResponse(UserProfile profile) {
        return new UserResponse()
                .id(profile.getId())
                .username(profile.getUsername())
                .displayName(profile.getDisplayName())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .status(AccountStatus.fromValue(profile.getStatus().name()))
                .createdAt(profile.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(profile.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

    private NotificationPreferencesResponse toPreferencesResponse(
            com.skateboard.user.domain.model.NotificationPreferences preferences) {
        return new NotificationPreferencesResponse()
                .notifications(new NotificationPreferences()
                        .pushEnabled(preferences.isPushEnabled())
                        .newPodcastEnabled(preferences.isNewPodcastEnabled()))
                .updatedAt(preferences.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

    private ProblemReportResponse toProblemReportResponse(ProblemReport report) {
        return new ProblemReportResponse()
                .id(report.getId())
                .category(ProblemReportCategory.fromValue(report.getCategory().name()))
                .message(report.getMessage())
                .appVersion(report.getAppVersion())
                .platform(report.getPlatform() != null ? ProblemReportPlatform.fromValue(report.getPlatform().name()) : null)
                .createdAt(report.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
