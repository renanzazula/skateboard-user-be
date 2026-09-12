package com.skateboard.user.adapter.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the real Flyway-managed schema (V3__problem_reports.sql) against an
 * actual Postgres instance, including the FK to user_profile and its ON DELETE
 * CASCADE, which a plain unit test or an approximated in-memory DB could not
 * verify honestly.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProblemReportJpaEntityPersistenceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private SpringUserProfileRepository userRepository;

    @Autowired
    private SpringProblemReportRepository reportRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UserProfileJpaEntity persistedUser() {
        UserProfileJpaEntity user = new UserProfileJpaEntity();
        user.setId(UUID.randomUUID());
        user.setKeycloakUserId(UUID.randomUUID());
        user.setAccountStatus("ACTIVE");
        user.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        return userRepository.saveAndFlush(user);
    }

    private ProblemReportJpaEntity fullReport(UUID userId) {
        ProblemReportJpaEntity report = new ProblemReportJpaEntity();
        report.setId(UUID.randomUUID());
        report.setUserId(userId);
        report.setCategory("APP_ERROR");
        report.setMessage("app crashed on launch");
        report.setAppVersion("2.1.0");
        report.setPlatform("ANDROID");
        report.setCreatedAt(Instant.parse("2024-02-01T09:15:00Z"));
        return report;
    }

    @Test
    void persistsAndReloadsEveryColumn() {
        UserProfileJpaEntity user = persistedUser();
        ProblemReportJpaEntity report = fullReport(user.getId());

        reportRepository.saveAndFlush(report);
        entityManager.clear();

        ProblemReportJpaEntity reloaded = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(user.getId());
        assertThat(reloaded.getCategory()).isEqualTo("APP_ERROR");
        assertThat(reloaded.getMessage()).isEqualTo("app crashed on launch");
        assertThat(reloaded.getAppVersion()).isEqualTo("2.1.0");
        assertThat(reloaded.getPlatform()).isEqualTo("ANDROID");
        assertThat(reloaded.getCreatedAt()).isEqualTo(Instant.parse("2024-02-01T09:15:00Z"));
    }

    @Test
    void appVersionAndPlatform_canBothBeNull() {
        UserProfileJpaEntity user = persistedUser();
        ProblemReportJpaEntity report = fullReport(user.getId());
        report.setAppVersion(null);
        report.setPlatform(null);

        reportRepository.saveAndFlush(report);
        entityManager.clear();

        ProblemReportJpaEntity reloaded = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(reloaded.getAppVersion()).isNull();
        assertThat(reloaded.getPlatform()).isNull();
    }

    @Test
    void userId_mustReferenceAnExistingUser_perRealForeignKeyConstraint() {
        ProblemReportJpaEntity orphanReport = fullReport(UUID.randomUUID());

        assertThatThrownBy(() -> reportRepository.saveAndFlush(orphanReport))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void message_cannotBeNull_perRealSchemaConstraint() {
        UserProfileJpaEntity user = persistedUser();
        ProblemReportJpaEntity report = fullReport(user.getId());
        report.setMessage(null);

        assertThatThrownBy(() -> reportRepository.saveAndFlush(report))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingTheOwningUser_cascadeDeletesTheirProblemReports() {
        UserProfileJpaEntity user = persistedUser();
        ProblemReportJpaEntity report = reportRepository.saveAndFlush(fullReport(user.getId()));
        entityManager.clear();

        userRepository.delete(userRepository.findById(user.getId()).orElseThrow());
        userRepository.flush();
        entityManager.clear();

        assertThat(reportRepository.findById(report.getId())).isEmpty();
    }
}
