package com.skateboard.user.application.service;

import com.skateboard.user.application.port.out.IdentityProviderPort;
import com.skateboard.user.domain.model.IdentitySummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class LookupUsersServiceTest {

    @Mock
    private IdentityProviderPort identityProviderPort;

    private LookupUsersService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new LookupUsersService(identityProviderPort);
    }

    @Test
    void executeDelegatesToIdentityProviderPort() {
        UUID id = UUID.randomUUID();
        List<IdentitySummary> expected = List.of(new IdentitySummary(id, "user@example.com", true, true));
        when(identityProviderPort.findIdentities(List.of(id))).thenReturn(expected);

        List<IdentitySummary> result = service.execute(List.of(id));

        assertThat(result).isEqualTo(expected);
    }
}
