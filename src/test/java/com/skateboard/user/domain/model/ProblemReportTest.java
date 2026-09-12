package com.skateboard.user.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemReportTest {

    @Test
    void createGeneratesARandomIdAndTimestampsAndCopiesEveryField() {
        UUID userId = UUID.randomUUID();

        ProblemReport report = ProblemReport.create(
                userId, ProblemReportCategory.APP_ERROR, "Crashes on launch", "1.2.3", ProblemReportPlatform.ANDROID);

        assertThat(report.getId()).isNotNull();
        assertThat(report.getUserId()).isEqualTo(userId);
        assertThat(report.getCategory()).isEqualTo(ProblemReportCategory.APP_ERROR);
        assertThat(report.getMessage()).isEqualTo("Crashes on launch");
        assertThat(report.getAppVersion()).isEqualTo("1.2.3");
        assertThat(report.getPlatform()).isEqualTo(ProblemReportPlatform.ANDROID);
        assertThat(report.getCreatedAt()).isNotNull();
    }

    @Test
    void reconstituteRehydratesEveryFieldFromPersistenceUnchanged() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-03-01T12:00:00Z");

        ProblemReport report = ProblemReport.reconstitute(
                id, userId, ProblemReportCategory.CONTENT_ISSUE, "Bad content", "2.0.0",
                ProblemReportPlatform.IOS, createdAt);

        assertThat(report.getId()).isEqualTo(id);
        assertThat(report.getUserId()).isEqualTo(userId);
        assertThat(report.getCategory()).isEqualTo(ProblemReportCategory.CONTENT_ISSUE);
        assertThat(report.getMessage()).isEqualTo("Bad content");
        assertThat(report.getAppVersion()).isEqualTo("2.0.0");
        assertThat(report.getPlatform()).isEqualTo(ProblemReportPlatform.IOS);
        assertThat(report.getCreatedAt()).isEqualTo(createdAt);
    }
}
