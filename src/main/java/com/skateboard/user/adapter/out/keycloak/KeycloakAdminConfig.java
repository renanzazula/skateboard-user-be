package com.skateboard.user.adapter.out.keycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client-credentials admin client for Keycloak's Admin REST API. Building it
 * does not itself make a network call (the token is fetched lazily on first
 * admin operation), so — like SecurityConfig's JwtDecoder — app startup
 * doesn't couple to Keycloak being reachable at that exact moment.
 */
@Configuration
public class KeycloakAdminConfig {

    @Bean
    public Keycloak keycloakAdminClient(
            @Value("${app.security.oauth2.admin.server-url}") String serverUrl,
            @Value("${app.security.oauth2.admin.realm}") String realm,
            @Value("${app.security.oauth2.admin.client-id}") String clientId,
            @Value("${app.security.oauth2.admin.client-secret}") String clientSecret) {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
