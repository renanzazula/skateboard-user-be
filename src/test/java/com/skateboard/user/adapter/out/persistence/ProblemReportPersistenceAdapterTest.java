package com.skateboard.user.adapter.out.persistence;

import com.skateboard.user.domain.model.ProblemReport;
import com.skateboard.user.domain.model.ProblemReportCategory;
import com.skateboard.user.domain.model.ProblemReportPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProblemReportPersistenceAdapterTest {

    @Mock
    private SpringProblemReportRepository jpaRepository;

    private ProblemReportPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new ProblemReportPersistenceAdapter(jpaRepository);
    }

    @Test
    void save_mapsDomainToEntity_andReturnsDomainBuiltFromWhatRepositoryActuallySaved() {
        UUID userId = UUID.randomUUID();
        ProblemReport report = ProblemReport.create(userId, ProblemReportCategory.APP_ERROR, "crashes on launch",
                "1.2.3", ProblemReportPlatform.ANDROID);

        // The repository returns a *different* entity instance (simulating what the
        // persistence layer handed back) so the assertions below prove the adapter
        // reads the returned/saved entity, not the original request object.
        ProblemReportJpaEntity saved = new ProblemReportJpaEntity();
        saved.setId(report.getId());
        saved.setUserId(userId);
        saved.setCategory("APP_ERROR");
        saved.setMessage("crashes on launch (persisted)");
        saved.setAppVersion("1.2.3");
        saved.setPlatform("ANDROID");
        saved.setCreatedAt(report.getCreatedAt());
        when(jpaRepository.save(any())).thenReturn(saved);

        ProblemReport result = adapter.save(report);

        ArgumentCaptor<ProblemReportJpaEntity> captor = ArgumentCaptor.forClass(ProblemReportJpaEntity.class);
        verify(jpaRepository).save(captor.capture());
        ProblemReportJpaEntity passedToSave = captor.getValue();
        assertThat(passedToSave.getId()).isEqualTo(report.getId());
        assertThat(passedToSave.getUserId()).isEqualTo(userId);
        assertThat(passedToSave.getCategory()).isEqualTo("APP_ERROR");
        assertThat(passedToSave.getMessage()).isEqualTo("crashes on launch");
        assertThat(passedToSave.getAppVersion()).isEqualTo("1.2.3");
        assertThat(passedToSave.getPlatform()).isEqualTo("ANDROID");
        assertThat(passedToSave.getCreatedAt()).isEqualTo(report.getCreatedAt());

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCategory()).isEqualTo(ProblemReportCategory.APP_ERROR);
        assertThat(result.getMessage()).isEqualTo("crashes on launch (persisted)");
        assertThat(result.getAppVersion()).isEqualTo("1.2.3");
        assertThat(result.getPlatform()).isEqualTo(ProblemReportPlatform.ANDROID);
        assertThat(result.getCreatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void save_leavesPlatformNull_whenReportHasNoPlatform_bothOnEntityAndReturnedDomain() {
        UUID userId = UUID.randomUUID();
        ProblemReport report = ProblemReport.create(userId, ProblemReportCategory.OTHER, "no platform given",
                null, null);
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProblemReport result = adapter.save(report);

        ArgumentCaptor<ProblemReportJpaEntity> captor = ArgumentCaptor.forClass(ProblemReportJpaEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getPlatform()).isNull();
        assertThat(captor.getValue().getAppVersion()).isNull();

        assertThat(result.getPlatform()).isNull();
        assertThat(result.getAppVersion()).isNull();
        assertThat(result.getCategory()).isEqualTo(ProblemReportCategory.OTHER);
    }

    @Test
    void save_throws_whenSavedEntityCategoryIsNotAValidEnumValue() {
        UUID userId = UUID.randomUUID();
        ProblemReport report = ProblemReport.create(userId, ProblemReportCategory.ACCOUNT_ISSUE, "msg",
                null, ProblemReportPlatform.IOS);
        ProblemReportJpaEntity saved = new ProblemReportJpaEntity();
        saved.setId(report.getId());
        saved.setUserId(userId);
        saved.setCategory("NOT_A_REAL_CATEGORY");
        saved.setMessage("msg");
        saved.setPlatform("IOS");
        saved.setCreatedAt(Instant.now());
        when(jpaRepository.save(any())).thenReturn(saved);

        assertThatThrownBy(() -> adapter.save(report)).isInstanceOf(IllegalArgumentException.class);
    }
}
