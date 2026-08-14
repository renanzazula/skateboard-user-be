package com.skateboard.user.application.port.in;

import com.skateboard.user.domain.model.ProblemReport;
import com.skateboard.user.domain.model.ProblemReportCategory;
import com.skateboard.user.domain.model.ProblemReportPlatform;

import java.util.UUID;

public interface ReportProblemUseCase {

    ProblemReport execute(Input input);

    record Input(UUID keycloakUserId, ProblemReportCategory category, String message,
                 String appVersion, ProblemReportPlatform platform) {}
}
