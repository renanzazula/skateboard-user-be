package com.skateboard.user.adapter.in.rest;

import com.skateboard.application.dto.*;
import com.skateboard.infrastructure.web.api.MeApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Every operation additionally requires an operation-specific FUNC_USER_*
 * authority (see api/openapi.yaml's x-required-permissions), granted to both
 * the ADMIN and STANDARD realm roles — not GUEST — in the shared
 * realm-export. Scoping to the caller's own JWT subject (see
 * currentUserId()) is what makes this "self-service"; the permission check
 * on top is what keeps unauthenticated/guest sessions out, per
 * skateboard-fe's Settings README ("Frontend permission checks are for UX.
 * Backend permission checks are for security.").
 */
@RestController
public class UserController implements MeApi {

    private static final String SELF_READ = "hasAuthority('FUNC_USER_SELF_READ')";
    private static final String SELF_UPDATE = "hasAuthority('FUNC_USER_SELF_UPDATE')";
    private static final String ACCOUNT_DELETE = "hasAuthority('FUNC_USER_ACCOUNT_DELETE')";
    private static final String PASSWORD_CHANGE = "hasAuthority('FUNC_USER_PASSWORD_CHANGE')";
    private static final String ACCOUNT_DEACTIVATE = "hasAuthority('FUNC_USER_ACCOUNT_DEACTIVATE')";
    private static final String PROBLEM_REPORT_CREATE = "hasAuthority('FUNC_USER_PROBLEM_REPORT_CREATE')";

    private final UserFacadeService userFacadeService;

    public UserController(UserFacadeService userFacadeService) {
        this.userFacadeService = userFacadeService;
    }

    @Override
    @PreAuthorize(SELF_READ)
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userFacadeService.getCurrentUser(currentUserId(), currentUsernameHint()));
    }

    @Override
    @PreAuthorize(SELF_UPDATE)
    public ResponseEntity<UserResponse> updateCurrentUser(UpdateUserRequest updateUserRequest) {
        return ResponseEntity.ok(userFacadeService.updateCurrentUser(currentUserId(), updateUserRequest));
    }

    @Override
    @PreAuthorize(ACCOUNT_DELETE)
    public ResponseEntity<Void> deleteCurrentUser() {
        userFacadeService.deleteCurrentUser(currentUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize(SELF_READ)
    public ResponseEntity<NotificationPreferencesResponse> getNotificationPreferences() {
        return ResponseEntity.ok(userFacadeService.getNotificationPreferences(currentUserId()));
    }

    @Override
    @PreAuthorize(SELF_UPDATE)
    public ResponseEntity<NotificationPreferencesResponse> updateNotificationPreferences(
            UpdateNotificationPreferencesRequest updateNotificationPreferencesRequest) {
        return ResponseEntity.ok(
                userFacadeService.updateNotificationPreferences(currentUserId(), updateNotificationPreferencesRequest));
    }

    @Override
    @PreAuthorize(SELF_UPDATE)
    public ResponseEntity<UserResponse> uploadProfilePicture(MultipartFile file) {
        return ResponseEntity.ok(userFacadeService.uploadProfilePicture(currentUserId(), file));
    }

    @Override
    @PreAuthorize(SELF_UPDATE)
    public ResponseEntity<UserResponse> changeUsername(ChangeUsernameRequest changeUsernameRequest) {
        return ResponseEntity.ok(userFacadeService.changeUsername(currentUserId(), changeUsernameRequest));
    }

    @Override
    @PreAuthorize(PASSWORD_CHANGE)
    public ResponseEntity<Void> changePassword(ChangePasswordRequest changePasswordRequest) {
        userFacadeService.changePassword(currentUserId(), changePasswordRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize(ACCOUNT_DEACTIVATE)
    public ResponseEntity<UserResponse> deactivateCurrentUser() {
        return ResponseEntity.ok(userFacadeService.deactivateCurrentUser(currentUserId()));
    }

    @Override
    @PreAuthorize(PROBLEM_REPORT_CREATE)
    public ResponseEntity<ProblemReportResponse> reportProblem(ProblemReportRequest problemReportRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userFacadeService.reportProblem(currentUserId(), problemReportRequest));
    }

    // The caller never chooses which account is affected — every operation is
    // scoped to the subject of the caller's own JWT (README: "PATCH /me", not
    // "PATCH /users/{userId}").
    private UUID currentUserId() {
        return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    /** Used only as a display-name fallback the first time a profile is provisioned. */
    private String currentUsernameHint() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            Object preferredUsername = jwtAuth.getToken().getClaims().get("preferred_username");
            return preferredUsername != null ? preferredUsername.toString() : null;
        }
        return null;
    }
}
