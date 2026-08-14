package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.ReportProblemUseCase;
import com.skateboard.user.application.port.out.ProblemReportRepositoryPort;
import com.skateboard.user.domain.model.ProblemReport;
import com.skateboard.user.domain.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class ReportProblemService implements ReportProblemUseCase {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final ProblemReportRepositoryPort problemReportRepositoryPort;

    public ReportProblemService(GetCurrentUserUseCase getCurrentUserUseCase,
                                 ProblemReportRepositoryPort problemReportRepositoryPort) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.problemReportRepositoryPort = problemReportRepositoryPort;
    }

    @Override
    public ProblemReport execute(Input input) {
        UserProfile profile = getCurrentUserUseCase.execute(input.keycloakUserId(), null);
        ProblemReport report = ProblemReport.create(
                profile.getId(), input.category(), input.message(), input.appVersion(), input.platform());
        return problemReportRepositoryPort.save(report);
    }
}
