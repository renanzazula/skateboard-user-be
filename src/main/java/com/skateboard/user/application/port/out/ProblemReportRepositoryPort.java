package com.skateboard.user.application.port.out;

import com.skateboard.user.domain.model.ProblemReport;

public interface ProblemReportRepositoryPort {
    ProblemReport save(ProblemReport report);
}
