package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SecretAndServiceMeshSecurityIT {
    @Test
    void shouldExternalizeRuntimeSecretsAndEnforceStrictMtls() throws IOException {
        String externalSecret =
                Files.readString(Path.of("..", "ops", "k8s", "external-secrets", "runtime-secret.yml").normalize());
        String serviceMesh =
                Files.readString(Path.of("..", "ops", "k8s", "service-mesh", "istio-mtls.yml").normalize());

        assertThat(externalSecret).contains("kind: SecretStore").contains("kind: ExternalSecret")
                .contains("refreshInterval: 1h").contains("secretKey: EMALL_DB_USERNAME")
                .contains("secretKey: EMALL_DB_PASSWORD").contains("secretKey: EMALL_FIELD_ENCRYPTION_KEY")
                .contains("secretKey: EMALL_INTERNAL_OPERATIONS_TOKEN").contains("remoteRef:");
        assertThat(serviceMesh).contains("kind: PeerAuthentication").contains("mode: STRICT")
                .contains("kind: DestinationRule").contains("mode: ISTIO_MUTUAL").contains("kind: AuthorizationPolicy")
                .contains("cluster.local/ns/emall/sa/gateway").contains("cluster.local/ns/emall/sa/order")
                .contains("cluster.local/ns/emall/sa/payment");
    }
}
