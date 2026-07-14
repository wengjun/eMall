package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class KubernetesRuntimeManifestIT {
    private static final Path MANIFEST_DIR = Path.of("..", "ops", "k8s").normalize();
    private static final Path REPOSITORY_ROOT = Path.of("..").normalize();
    private static final List<String> STABLE_SERVICES = List.of("gateway", "user", "product", "inventory", "order",
            "cart", "payment", "pricing", "marketing", "search", "fulfillment", "review", "after-sales");
    private static final List<String> MIGRATION_RUNNER_SERVICES = List.of("advertising", "after-sales", "analytics",
            "cart", "catalog", "cost", "customer-service", "data-warehouse", "event-platform", "experiment", "finance",
            "flash-sale", "forecasting", "fulfillment", "identity", "intelligence", "inventory", "marketing",
            "merchant", "openapi", "operations", "order", "payment", "platform-ops", "pricing", "product", "promotion",
            "recommendation", "release", "reliability", "review", "risk", "search", "supply-chain", "traffic", "user");

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
    void shouldParseEveryKubernetesManifestAsVersionedResources() throws IOException {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        List<Path> manifests;
        try (var paths = Files.walk(MANIFEST_DIR)) {
            manifests = paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".yml")).sorted()
                    .toList();
        }

        for (Path manifest : manifests) {
            List<Object> documents = new ArrayList<>();
            yaml.loadAll(Files.readString(manifest)).forEach(documents::add);
            assertThat(documents).as("YAML documents in %s", manifest).isNotEmpty();
            for (Object document : documents) {
                assertThat(document).as("resource in %s", manifest).isInstanceOf(Map.class);
                Map<?, ?> resource = (Map<?, ?>) document;
                assertThat(resource.containsKey("apiVersion")).as("apiVersion in %s", manifest).isTrue();
                assertThat(resource.containsKey("kind")).as("kind in %s", manifest).isTrue();
            }
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
                .contains("name: EMALL_MIGRATION_JDBC_URL_TEMPLATE").contains("jdbc:mysql://mysql:3306/{database}")
                .contains("name: EMALL_MIGRATION_LOCATIONS")
                .contains("classpath:migrations/{service}/src/main/resources/db/migration")
                .contains("name: EMALL_MIGRATION_CREATE_PHYSICAL_TABLES").contains("secretRef:")
                .contains("name: emall-runtime-secret");
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
