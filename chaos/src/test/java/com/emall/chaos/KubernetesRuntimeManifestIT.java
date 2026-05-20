package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class KubernetesRuntimeManifestIT {
    private static final Path MANIFEST_DIR = Path.of("..", "ops", "k8s").normalize();
    private static final Path REPOSITORY_ROOT = Path.of("..").normalize();
    private static final List<String> STABLE_SERVICES = List.of("gateway", "user", "product", "inventory", "order",
            "cart", "payment", "pricing", "marketing", "search", "fulfillment", "review", "after-sales");
    private static final List<String> MIGRATION_RUNNER_SERVICES =
            List.of("order", "inventory", "payment", "product", "pricing", "search", "user", "cart", "flash-sale");

    @Test
    void shouldKeepStableRuntimeManifestsDeployableAndOperable() throws IOException {
        String serviceAccounts = Files.readString(MANIFEST_DIR.resolve("service-accounts.yml"));
        String networkPolicy = Files.readString(MANIFEST_DIR.resolve("network-policy.yml"));

        for (String service : STABLE_SERVICES) {
            Path manifest = MANIFEST_DIR.resolve(service + ".yml");
            assertThat(manifest).exists().isRegularFile();

            String content = Files.readString(manifest);
            assertThat(content).contains("kind: Deployment").contains("kind: Service")
                    .contains("kind: PodDisruptionBudget").contains("kind: HorizontalPodAutoscaler")
                    .contains("serviceAccountName: " + service).contains("runAsNonRoot: true")
                    .contains("allowPrivilegeEscalation: false").contains("readinessProbe:").contains("livenessProbe:")
                    .contains("resources:").contains("requests:").contains("limits:").contains("memory:");
            assertThat(serviceAccounts).contains("name: " + service);
            assertThat(networkPolicy).as("network policy for %s", service).containsAnyOf("app: " + service,
                    "- " + service);
        }
    }

    @Test
    void shouldProvideHelmDeploymentBaselineForStableRuntime() throws IOException {
        Path chartDir = Path.of("..", "ops", "helm", "emall").normalize();
        String chart = Files.readString(chartDir.resolve("Chart.yaml"));
        String values = Files.readString(chartDir.resolve("values.yaml"));
        String deployment = Files.readString(chartDir.resolve("templates/deployment.yaml"));
        String hpa = Files.readString(chartDir.resolve("templates/hpa.yaml"));

        assertThat(chart).contains("name: emall").contains("type: application");
        assertThat(values).contains("EMALL_SENTINEL_ENABLED").contains("EMALL_REDIS_CLUSTER_NODES")
                .contains("EMALL_NACOS_DISCOVERY_ENABLED").contains("EMALL_DUBBO_REGISTRY_ADDRESS")
                .contains("name: order").contains("name: payment");
        assertThat(deployment).contains("SPRING_PROFILES_ACTIVE").contains("redis-cluster")
                .contains("/actuator/health/readiness").contains("/actuator/health/liveness");
        assertThat(hpa).contains("kind: HorizontalPodAutoscaler").contains("averageUtilization");
    }

    @Test
    void shouldExposePublicTrafficThroughGatewayApiAndAlb() throws IOException {
        Path gatewayApi = MANIFEST_DIR.resolve("gateway-api.yml");

        assertThat(gatewayApi).exists().isRegularFile();
        assertThat(MANIFEST_DIR.resolve("ingress.yml")).doesNotExist();

        String content = Files.readString(gatewayApi);
        assertThat(content).contains("apiVersion: gateway.networking.k8s.io/v1").contains("kind: Gateway")
                .contains("gatewayClassName: alb").contains("protocol: HTTPS").contains("mode: Terminate")
                .contains("kind: HTTPRoute").contains("RequestRedirect").contains("scheme: https")
                .contains("statusCode: 301").contains("ResponseHeaderModifier").contains("Strict-Transport-Security")
                .contains("backendRefs:").contains("name: gateway").contains("port: 8080");
        assertThat(content).doesNotContain("kind: Ingress").doesNotContain("nginx.ingress.kubernetes.io");
    }

    @Test
    void shouldProvideDedicatedMigrationRunnerJobForShardSchemaRollouts() throws IOException {
        Path runnerManifest = MANIFEST_DIR.resolve("migration-runner.yml");
        String runner = Files.readString(runnerManifest);
        String serviceAccounts = Files.readString(MANIFEST_DIR.resolve("service-accounts.yml"));
        String physicalTableTemplate =
                Files.readString(Path.of("..", "ops", "mysql", "sharding", "physical-table-template.sql").normalize());

        assertThat(runnerManifest).exists().isRegularFile();
        assertThat(runner).contains("apiVersion: batch/v1").contains("kind: Job")
                .contains("name: emall-migration-runner").contains("backoffLimit: 0").contains("restartPolicy: Never")
                .contains("serviceAccountName: emall-migration-runner").contains("image: emall/migration-runner:latest")
                .contains("name: EMALL_MIGRATION_OPERATOR").contains("fieldPath: metadata.annotations")
                .contains("name: EMALL_MIGRATION_JDBC_URL_TEMPLATE")
                .contains("mysql-{region}-{shard}.emall-db:3306/emall_{service}_{shard}")
                .contains("name: EMALL_MIGRATION_LOCATIONS").contains("filesystem:/migrations/{service}")
                .contains("secretRef:").contains("name: emall-db-credential");
        assertThat(serviceAccounts).contains("name: emall-migration-runner")
                .contains("automountServiceAccountToken: true");
        for (String service : MIGRATION_RUNNER_SERVICES) {
            assertThat(runner).as("migration runner service target %s", service).contains(service);
            assertThat(REPOSITORY_ROOT.resolve(service).resolve("src/main/resources/db/migration"))
                    .as("migration directory for %s", service).isDirectory();
        }
        assertThat(physicalTableTemplate).contains("CREATE TABLE {table}_{index} LIKE {source_table}")
                .contains("CHECK (shard_id = {shard_id})").contains("CHECK (cell_id = '{cell_id}')");
    }
}
