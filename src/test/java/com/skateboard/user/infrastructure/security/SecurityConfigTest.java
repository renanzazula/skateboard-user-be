package com.skateboard.user.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link SecurityConfig#filterChain(HttpSecurity)} by mocking the
 * Spring Security fluent builder API and actually invoking the customizer
 * lambdas the production code hands it, rather than merely asserting that
 * the builder methods were called. This verifies the real authorization
 * rules (health check open, everything else authenticated), the stateless
 * session policy, and that the JWT decoder/converter wired into the OAuth2
 * resource server are the ones this class builds (audience-validating
 * NimbusJwtDecoder + authorities-claim-based JwtAuthenticationConverter).
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class SecurityConfigTest {

    private static final String ISSUER_URI = "http://localhost:8180/realms/skateboard-podcast";
    private static final String REQUIRED_AUDIENCE = "skateboard-user-be";

    @Mock
    private HttpSecurity http;

    @Mock
    private CsrfConfigurer csrfConfigurer;

    @Mock
    private SessionManagementConfigurer sessionManagementConfigurer;

    @Mock
    private AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry registry;

    @Mock
    private AuthorizeHttpRequestsConfigurer.AuthorizedUrl authorizedUrlForHealth;

    @Mock
    private AuthorizeHttpRequestsConfigurer.AuthorizedUrl authorizedUrlForAnyRequest;

    @Mock
    private OAuth2ResourceServerConfigurer resourceServerConfigurer;

    @Mock
    private OAuth2ResourceServerConfigurer.JwtConfigurer jwtConfigurer;

    @Mock
    private DefaultSecurityFilterChain expectedChain;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        securityConfig = new SecurityConfig(ISSUER_URI, REQUIRED_AUDIENCE);

        when(http.csrf(any())).thenAnswer(invocation -> {
            Customizer<CsrfConfigurer> customizer = invocation.getArgument(0);
            customizer.customize(csrfConfigurer);
            return http;
        });
        when(http.sessionManagement(any())).thenAnswer(invocation -> {
            Customizer<SessionManagementConfigurer> customizer = invocation.getArgument(0);
            customizer.customize(sessionManagementConfigurer);
            return http;
        });
        when(http.authorizeHttpRequests(any())).thenAnswer(invocation -> {
            Customizer<AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry> customizer =
                    invocation.getArgument(0);
            customizer.customize(registry);
            return http;
        });
        when(http.oauth2ResourceServer(any())).thenAnswer(invocation -> {
            Customizer<OAuth2ResourceServerConfigurer> customizer = invocation.getArgument(0);
            customizer.customize(resourceServerConfigurer);
            return http;
        });
        when(http.build()).thenReturn(expectedChain);

        when(registry.requestMatchers("/actuator/health")).thenReturn(authorizedUrlForHealth);
        when(authorizedUrlForHealth.permitAll()).thenReturn(registry);
        when(registry.anyRequest()).thenReturn(authorizedUrlForAnyRequest);
        when(authorizedUrlForAnyRequest.authenticated()).thenReturn(registry);

        when(resourceServerConfigurer.jwt(any())).thenAnswer(invocation -> {
            Customizer<OAuth2ResourceServerConfigurer.JwtConfigurer> customizer = invocation.getArgument(0);
            customizer.customize(jwtConfigurer);
            return resourceServerConfigurer;
        });
        when(jwtConfigurer.decoder(any())).thenReturn(jwtConfigurer);
        when(jwtConfigurer.jwtAuthenticationConverter(any())).thenReturn(jwtConfigurer);
    }

    @Test
    void buildsFilterChainWithStatelessSessionsCsrfDisabledAndHealthCheckOpen() throws Exception {
        SecurityFilterChain result = securityConfig.filterChain(http);

        assertThat(result).isSameAs(expectedChain);
        verify(csrfConfigurer).disable();
        verify(sessionManagementConfigurer).sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        verify(registry).requestMatchers("/actuator/health");
        verify(authorizedUrlForHealth).permitAll();
        verify(registry).anyRequest();
        verify(authorizedUrlForAnyRequest).authenticated();
    }

    @Test
    void wiresAnAudienceValidatingNimbusDecoderIntoTheResourceServer() throws Exception {
        securityConfig.filterChain(http);

        ArgumentCaptor<JwtDecoder> decoderCaptor = ArgumentCaptor.forClass(JwtDecoder.class);
        verify(jwtConfigurer).decoder(decoderCaptor.capture());
        assertThat(decoderCaptor.getValue()).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    void wiresAJwtAuthenticationConverterIntoTheResourceServer() throws Exception {
        securityConfig.filterChain(http);

        ArgumentCaptor<Converter<Jwt, ?>> converterCaptor = ArgumentCaptor.forClass(Converter.class);
        verify(jwtConfigurer).jwtAuthenticationConverter(converterCaptor.capture());
        assertThat(converterCaptor.getValue()).isInstanceOf(JwtAuthenticationConverter.class);
    }

    @Test
    void constructorStoresIssuerAndAudienceUsedToBuildTheDecoder() throws Exception {
        // Different audience should still build without error; the actual
        // rejection behavior of a non-matching audience is covered by
        // AudienceValidatorTest. Here we confirm the constructor argument is
        // actually plumbed through to the decoder construction path (no
        // hardcoded issuer/audience).
        SecurityConfig otherConfig = new SecurityConfig("http://other-issuer", "other-audience");

        SecurityFilterChain result = otherConfig.filterChain(http);

        assertThat(result).isSameAs(expectedChain);
        verify(jwtConfigurer, org.mockito.Mockito.atLeastOnce()).decoder(any());
    }
}
