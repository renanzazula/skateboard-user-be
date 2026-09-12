package com.skateboard.user.adapter.out.keycloak;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class KeycloakAdminConfigTest {

    private final KeycloakAdminConfig config = new KeycloakAdminConfig();

    @Test
    void buildsAClientCredentialsAdminClientFromConfiguredProperties() {
        Keycloak client = config.keycloakAdminClient(
                "http://localhost:8180",
                "skateboard-podcast",
                "skateboard-user-be",
                "test-client-secret");

        assertThat(client).isNotNull();
    }

    @Test
    void buildingTheClientDoesNotEagerlyContactKeycloak() {
        // Building the client is expected to be a pure in-memory construction —
        // no network call happens until the first admin operation is invoked
        // (see class javadoc), so this must succeed even with an unreachable
        // server URL and without any running Keycloak instance.
        assertThatCode(() -> config.keycloakAdminClient(
                        "http://unreachable-host.invalid:8180",
                        "some-realm",
                        "some-client-id",
                        "some-client-secret"))
                .doesNotThrowAnyException();
    }
}
