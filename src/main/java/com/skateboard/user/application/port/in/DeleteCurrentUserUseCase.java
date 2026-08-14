package com.skateboard.user.application.port.in;

import java.util.UUID;

public interface DeleteCurrentUserUseCase {
    /** Idempotent — repeat calls after the account is already deleted are a no-op. */
    void execute(UUID keycloakUserId);
}
