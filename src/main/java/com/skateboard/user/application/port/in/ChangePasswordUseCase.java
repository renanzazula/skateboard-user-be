package com.skateboard.user.application.port.in;

import java.util.UUID;

public interface ChangePasswordUseCase {

    void execute(Input input);

    record Input(UUID keycloakUserId, String newPassword) {}
}
