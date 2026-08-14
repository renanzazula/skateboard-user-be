package com.skateboard.user.adapter.out.persistence;

import com.skateboard.user.application.port.out.ProblemReportRepositoryPort;
import com.skateboard.user.domain.model.ProblemReport;
import com.skateboard.user.domain.model.ProblemReportCategory;
import com.skateboard.user.domain.model.ProblemReportPlatform;
import org.springframework.stereotype.Component;

@Component
public class ProblemReportPersistenceAdapter implements ProblemReportRepositoryPort {

    private final SpringProblemReportRepository jpaRepository;

    public ProblemReportPersistenceAdapter(SpringProblemReportRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ProblemReport save(ProblemReport report) {
        ProblemReportJpaEntity entity = new ProblemReportJpaEntity();
        entity.setId(report.getId());
        entity.setUserId(report.getUserId());
        entity.setCategory(report.getCategory().name());
        entity.setMessage(report.getMessage());
        entity.setAppVersion(report.getAppVersion());
        entity.setPlatform(report.getPlatform() != null ? report.getPlatform().name() : null);
        entity.setCreatedAt(report.getCreatedAt());
        ProblemReportJpaEntity saved = jpaRepository.save(entity);
        return ProblemReport.reconstitute(
                saved.getId(), saved.getUserId(), ProblemReportCategory.valueOf(saved.getCategory()),
                saved.getMessage(), saved.getAppVersion(),
                saved.getPlatform() != null ? ProblemReportPlatform.valueOf(saved.getPlatform()) : null,
                saved.getCreatedAt());
    }
}
