package com.skateboard.user.adapter.in.rest;

import com.skateboard.application.admindto.UserLookupResponse;
import com.skateboard.user.application.port.in.LookupUsersUseCase;
import com.skateboard.user.domain.model.IdentitySummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

    @Mock
    private LookupUsersUseCase lookupUsersUseCase;

    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AdminUserController(lookupUsersUseCase);
    }

    @Test
    void lookupUsersMapsIdentitySummariesToResponseDtos() {
        UUID id = UUID.randomUUID();
        when(lookupUsersUseCase.execute(List.of(id)))
                .thenReturn(List.of(new IdentitySummary(id, "user@example.com", true, false)));

        ResponseEntity<List<UserLookupResponse>> response = controller.lookupUsers(List.of(id));

        assertThat(response.getBody()).containsExactly(
                new UserLookupResponse().id(id).email("user@example.com").emailVerified(true).active(false));
    }
}
