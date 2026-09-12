package com.skateboard.user.application.service;

import com.skateboard.user.application.port.in.GetCurrentUserUseCase;
import com.skateboard.user.application.port.in.ReportProblemUseCase;
import com.skateboard.user.application.port.out.ProblemReportRepositoryPort;
import com.skateboard.user.domain.model.ProblemReport;
import com.skateboard.user.domain.model.ProblemReportCategory;
import com.skateboard.user.domain.model.ProblemReportPlatform;
import com.skateboard.user.domain.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportProblemServiceTest {

    @Mock
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @Mock
    private ProblemReportRepositoryPort problemReportRepositoryPort;

    private ReportProblemService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ReportProblemService(getCurrentUserUseCase, problemReportRepositoryPort);
    }

    @Test
    void createsAReportScopedToTheCallersProfileAndPersistsIt() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(problemReportRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProblemReport result = service.execute(new ReportProblemUseCase.Input(
                keycloakUserId, ProblemReportCategory.APP_ERROR, "Crashes on launch", "1.2.3",
                ProblemReportPlatform.ANDROID));

        assertThat(result.getUserId()).isEqualTo(profile.getId());
        assertThat(result.getCategory()).isEqualTo(ProblemReportCategory.APP_ERROR);
        assertThat(result.getMessage()).isEqualTo("Crashes on launch");
        assertThat(result.getAppVersion()).isEqualTo("1.2.3");
        assertThat(result.getPlatform()).isEqualTo(ProblemReportPlatform.ANDROID);

        ArgumentCaptor<ProblemReport> captor = ArgumentCaptor.forClass(ProblemReport.class);
        verify(problemReportRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(profile.getId());
    }

    @Test
    void returnsWhateverTheRepositoryHandsBackRatherThanTheUnsavedInstance() {
        UUID keycloakUserId = UUID.randomUUID();
        UserProfile profile = UserProfile.provision(keycloakUserId, "rzazula");
        ProblemReport persisted = ProblemReport.reconstitute(UUID.randomUUID(), profile.getId(),
                ProblemReportCategory.OTHER, "message", "1.0.0", ProblemReportPlatform.IOS, java.time.Instant.now());
        when(getCurrentUserUseCase.execute(keycloakUserId, null)).thenReturn(profile);
        when(problemReportRepositoryPort.save(any())).thenReturn(persisted);

        ProblemReport result = service.execute(new ReportProblemUseCase.Input(
                keycloakUserId, ProblemReportCategory.OTHER, "message", "1.0.0", ProblemReportPlatform.IOS));

        assertThat(result).isSameAs(persisted);
    }
}
