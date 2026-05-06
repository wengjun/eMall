package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class KubernetesRuntimeManifestIT {
    private static final Path MANIFEST_DIR = Path.of("..", "ops", "k8s").normalize();
    private static final List<String> STABLE_SERVICES = List.of(
            "gateway", "user", "product", "inventory", "order", "cart", "payment", "pricing", "marketing",
            "search", "fulfillment", "review", "after-sales");

    @Test
    void shouldKeepStableRuntimeManifestsDeployableAndOperable() throws IOException {
        String serviceAccounts = Files.readString(MANIFEST_DIR.resolve("service-accounts.yml"));
        String networkPolicy = Files.readString(MANIFEST_DIR.resolve("network-policy.yml"));

        for (String service : STABLE_SERVICES) {
            Path manifest = MANIFEST_DIR.resolve(service + ".yml");
            assertThat(manifest).exists().isRegularFile();

            String content = Files.readString(manifest);
            assertThat(content)
                    .contains("kind: Deployment")
                    .contains("kind: Service")
                    .contains("kind: PodDisruptionBudget")
                    .contains("kind: HorizontalPodAutoscaler")
                    .contains("serviceAccountName: " + service)
                    .contains("runAsNonRoot: true")
                    .contains("allowPrivilegeEscalation: false")
                    .contains("readinessProbe:")
                    .contains("livenessProbe:")
                    .contains("resources:")
                    .contains("requests:")
                    .contains("limits:")
                    .contains("memory:");
            assertThat(serviceAccounts).contains("name: " + service);
            assertThat(networkPolicy)
                    .as("network policy for %s", service)
                    .containsAnyOf("app: " + service, "- " + service);
        }
    }
}
