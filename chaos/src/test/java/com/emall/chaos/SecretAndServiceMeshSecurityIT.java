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
        String runtimeConfig = Files.readString(Path.of("..", "ops", "k8s", "config.yml").normalize());
        String networkPolicy = Files.readString(Path.of("..", "ops", "k8s", "network-policy.yml").normalize());

        assertThat(externalSecret).contains("kind: ExternalSecret").contains("kind: ClusterSecretStore")
                .contains("name: emall-platform-vault").contains("refreshInterval: 1h")
                .contains("secretKey: EMALL_DB_USERNAME").contains("secretKey: EMALL_DB_PASSWORD")
                .contains("secretKey: EMALL_FIELD_ENCRYPTION_KEY")
                .contains("secretKey: EMALL_INTERNAL_OPERATIONS_TOKEN")
                .contains("secretKey: EMALL_SECURITY_AUTH_TOKEN_SECRET")
                .contains("secretKey: EMALL_PAYMENT_CALLBACK_SECRET")
                .contains("secretKey: EMALL_PAYMENT_CHANNEL_API_KEY").contains("remoteRef:")
                .doesNotContain("example.com", "kind: SecretStore");
        assertThat(serviceMesh).contains("kind: PeerAuthentication").contains("mode: STRICT")
                .contains("name: gateway-public-ingress").contains("mode: PERMISSIVE").contains("kind: DestinationRule")
                .contains("mode: ISTIO_MUTUAL").contains("kind: AuthorizationPolicy").contains("name: default-deny")
                .contains("emall.io/gateway-reachable: \"true\"").contains("emall.io/rpc-provider: \"true\"")
                .contains("ports: [\"20880\"]").contains("cluster.local/ns/emall/sa/gateway")
                .contains("cluster.local/ns/emall/sa/order").contains("cluster.local/ns/emall/sa/payment")
                .contains("cluster.local/ns/emall/sa/flash-sale");
        assertThat(networkPolicy).contains("name: allow-checkout-services-to-identity")
                .contains("name: allow-checkout-services-to-risk").contains("port: 8097").contains("port: 8098")
                .contains("name: allow-prometheus-scraping").contains("kubernetes.io/metadata.name: monitoring");
        assertThat(runtimeConfig).doesNotContain("kind: Secret", "root\n", "replace-in-production")
                .contains("EMALL_RUNTIME_MODE: production").contains("EMALL_DUBBO_PORT: \"20880\"")
                .contains("EMALL_TRUST_IDENTITY_ENABLED: \"true\"").contains("EMALL_TRUST_RISK_ENABLED: \"true\"");
    }
}
