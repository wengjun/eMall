package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SecretAndServiceMeshSecurityIT {
    private static final Path REPOSITORY_ROOT = Path.of("..").normalize();
    private static final Path CHART_DIR = REPOSITORY_ROOT.resolve("ops/helm/emall");

    @Test
    void shouldExternalizeRuntimeSecretsAndEnforceStrictMtls() throws IOException {
        String externalSecret =
                Files.readString(REPOSITORY_ROOT.resolve("ops/k8s/external-secrets/runtime-secret.yml"));
        String serviceMesh = Files.readString(CHART_DIR.resolve("templates/service-mesh.yaml"));
        String networkPolicy = Files.readString(CHART_DIR.resolve("templates/network-policy.yaml"));
        String runtimeConfig = Files.readString(CHART_DIR.resolve("templates/configmap.yaml"));
        String rollout = Files.readString(CHART_DIR.resolve("templates/rollout.yaml"));
        String values = Files.readString(CHART_DIR.resolve("values.yaml"));

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
                .contains("mode: ISTIO_MUTUAL").contains("kind: AuthorizationPolicy")
                .contains("name: emall-default-deny").contains("emall.io/gateway-reachable: \"true\"")
                .contains("emall.io/rpc-provider: \"true\"").contains("ports:").contains("- \"20880\"")
                .contains("/sa/gateway").contains("/sa/order").contains("/sa/payment");
        assertThat(networkPolicy).contains("name: emall-default-deny-ingress")
                .contains("name: allow-public-ingress-to-gateway").contains("name: allow-gateway-to-{{ .name }}")
                .contains("name: allow-order-to-rpc-providers").contains("name: allow-payment-to-order-rpc")
                .contains("name: allow-prometheus-scraping");
        assertThat(runtimeConfig).contains("kind: ConfigMap").doesNotContain("kind: Secret", "EMALL_DB_PASSWORD")
                .doesNotContain("EMALL_SECURITY_AUTH_TOKEN_SECRET", "EMALL_PAYMENT_CALLBACK_SECRET");
        assertThat(rollout).contains("configMapRef:").contains("secretRef:")
                .contains("name: {{ $.Values.global.existingSecret }}");
        assertThat(values).doesNotContain("replace-in-production").contains("EMALL_RUNTIME_MODE: production")
                .contains("EMALL_DUBBO_PORT: \"20880\"").contains("EMALL_TRUST_IDENTITY_ENABLED: \"true\"")
                .contains("EMALL_TRUST_RISK_ENABLED: \"true\"");
    }
}
