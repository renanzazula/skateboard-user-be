package com.skateboard.user.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AudienceValidatorTest {

    private static final String REQUIRED_AUDIENCE = "skateboard-user-be";

    @Mock
    private Jwt jwt;

    private AudienceValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new AudienceValidator(REQUIRED_AUDIENCE);
    }

    @Test
    void succeedsWhenRequiredAudienceIsPresent() {
        when(jwt.getAudience()).thenReturn(List.of("some-other-client", REQUIRED_AUDIENCE));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void failsWhenAudienceClaimIsMissingEntirely() {
        when(jwt.getAudience()).thenReturn(List.of());

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
        assertThat(result.getErrors().iterator().next().getDescription())
                .isEqualTo("Required audience is missing");
    }

    @Test
    void failsWhenAudienceContainsOnlyOtherClients() {
        when(jwt.getAudience()).thenReturn(List.of("skateboard-podcast-be", "skateboard-ui-backend"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }
}
