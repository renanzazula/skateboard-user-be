package com.skateboard.user.adapter.in.rest;

import com.skateboard.application.admindto.UserLookupResponse;
import com.skateboard.infrastructure.web.adminapi.AdminUsersApi;
import com.skateboard.user.application.port.in.LookupUsersUseCase;
import com.skateboard.user.domain.model.IdentitySummary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only counterpart to UserController's self-service /me surface (see
 * api/admin-openapi.yaml's header for why this is a separate spec/package
 * from MeApi). FUNC_USER_ADMIN_LOOKUP is ADMIN-only in the shared
 * realm-export — this endpoint reveals other users' email addresses, so it
 * gets no STANDARD grant.
 */
@RestController
public class AdminUserController implements AdminUsersApi {

    private final LookupUsersUseCase lookupUsersUseCase;

    public AdminUserController(LookupUsersUseCase lookupUsersUseCase) {
        this.lookupUsersUseCase = lookupUsersUseCase;
    }

    @Override
    @PreAuthorize("hasAuthority('FUNC_USER_ADMIN_LOOKUP')")
    public ResponseEntity<List<UserLookupResponse>> lookupUsers(List<UUID> ids) {
        List<UserLookupResponse> response = lookupUsersUseCase.execute(ids).stream()
                .map(AdminUserController::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private static UserLookupResponse toResponse(IdentitySummary summary) {
        return new UserLookupResponse()
                .id(summary.keycloakUserId())
                .email(summary.email())
                .emailVerified(summary.emailVerified())
                .active(summary.active());
    }
}
