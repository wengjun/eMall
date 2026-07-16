package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class KubernetesRuntimeManifestIT {
    private static final Path REPOSITORY_ROOT = Path.of("..").normalize();
    private static final Path MANIFEST_DIR = REPOSITORY_ROOT.resolve("ops/k8s");
    private static final Path CHART_DIR = REPOSITORY_ROOT.resolve("ops/helm/emall");
    private static final List<String> ONLINE_SERVICES = List.of("gateway", "routing", "user", "product", "inventory",
            "order", "cart", "payment", "pricing", "marketing", "search", "fulfillment", "review", "after-sales",
            "merchant", "flash-sale", "recommendation", "cost", "identity", "risk", "operations", "openapi", "catalog",
            "promotion", "experiment", "advertising", "supply-chain", "finance", "customer-service", "forecasting",
            "event-platform", "data-warehouse", "intelligence", "analytics", "traffic", "reliability", "release",
            "platform-ops");
    private static final List<String> MIGRATION_RUNNER_SERVICES =
            ONLINE_SERVICES.stream().filter(service -> !service.equals("gateway")).sorted().toList();

    @Test
    void shouldUseHelmAsTheOnlyOnlineRuntimeSource() throws IOException {
        Map<String, Object> values = yamlMap(Files.readString(CHART_DIR.resolve("values.yaml")));
        List<Map<String, Object>> services = mapList(values.get("services"));
        List<String> names = services.stream().map(service -> String.valueOf(service.get("name"))).toList();
        Set<Integer> ports =
                services.stream().map(service -> ((Number) service.get("port")).intValue()).collect(Collectors.toSet());

        assertThat(names).containsExactlyElementsOf(ONLINE_SERVICES);
        assertThat(new HashSet<>(names)).hasSize(ONLINE_SERVICES.size());
        assertThat(ports).hasSize(ONLINE_SERVICES.size());
        for (String service : ONLINE_SERVICES) {
            assertThat(REPOSITORY_ROOT.resolve(service).resolve("pom.xml")).as("module %s", service).isRegularFile();
            assertThat(MANIFEST_DIR.resolve(service + ".yml")).as("duplicate raw manifest for %s", service)
                    .doesNotExist();
        }
        assertThat(MANIFEST_DIR.resolve("config.yml")).doesNotExist();
        assertThat(MANIFEST_DIR.resolve("service-accounts.yml")).doesNotExist();
        assertThat(MANIFEST_DIR.resolve("network-policy.yml")).doesNotExist();
        assertThat(MANIFEST_DIR.resolve("gateway-api.yml")).doesNotExist();
        assertDirectoryIsAbsentOrEmpty(MANIFEST_DIR.resolve("rollouts"));
        assertDirectoryIsAbsentOrEmpty(MANIFEST_DIR.resolve("service-mesh"));
    }

    @Test
    void shouldKeepProductionPoliciesInTheAuthoritativeChart() throws IOException {
        String chart = Files.readString(CHART_DIR.resolve("Chart.yaml"));
        String schema = Files.readString(CHART_DIR.resolve("values.schema.json"));
        String rollout = Files.readString(CHART_DIR.resolve("templates/rollout.yaml"));
        String hpa = Files.readString(CHART_DIR.resolve("templates/hpa.yaml"));
        String pdb = Files.readString(CHART_DIR.resolve("templates/poddisruptionbudget.yaml"));
        String network = Files.readString(CHART_DIR.resolve("templates/network-policy.yaml"));

        assertThat(chart).contains("emall.io/deployment-source: authoritative").contains("kubeVersion:");
        assertThat(schema).contains("\"minItems\": 38").contains("\"maxItems\": 38")
                .contains("\"not\": {\"enum\": [\"latest\"]}");
        assertThat(rollout).contains("kind: Rollout").contains("topologySpreadConstraints:")
                .contains("podAntiAffinity:").contains("runAsNonRoot: true").contains("readOnlyRootFilesystem: true")
                .contains("allowPrivilegeEscalation: false").contains("startupProbe:").contains("readinessProbe:")
                .contains("livenessProbe:").contains("terminationGracePeriodSeconds:").contains("preStop:")
                .contains("resources:").doesNotContain("kind: Deployment");
        assertThat(hpa).contains("kind: HorizontalPodAutoscaler").contains("behavior:").contains("name: memory")
                .contains("kind: Rollout");
        assertThat(pdb).contains("kind: PodDisruptionBudget").contains("unhealthyPodEvictionPolicy: AlwaysAllow");
        assertThat(network).contains("emall-default-deny-ingress").contains("allow-order-to-rpc-providers")
                .contains("allow-argo-rollouts-to-release").contains("allow-prometheus-scraping");
    }

    @Test
    void shouldParseEveryRemainingRawManifestAsVersionedResources() throws IOException {
        Yaml yaml = yaml();
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
    void shouldExposePublicTrafficThroughChartManagedGatewayApi() throws IOException {
        String gateway = Files.readString(CHART_DIR.resolve("templates/gateway-api.yaml"));

        assertThat(gateway).contains("apiVersion: gateway.networking.k8s.io/v1").contains("kind: Gateway")
                .contains("gatewayClassName:").contains("protocol: HTTPS").contains("mode: Terminate")
                .contains("kind: HTTPRoute").contains("RequestRedirect").contains("scheme: https")
                .contains("statusCode: 301").contains("ResponseHeaderModifier").contains("Strict-Transport-Security")
                .contains("backendRefs:").contains("name: gateway").contains("port: 8080")
                .doesNotContain("kind: Ingress").doesNotContain("nginx.ingress.kubernetes.io");
    }

    @Test
    void shouldIsolateEveryServiceMigrationArtifactJobAndCredential() throws IOException {
        String job = Files.readString(CHART_DIR.resolve("templates/migration-job.yaml"));
        String secret = Files.readString(CHART_DIR.resolve("templates/migration-external-secret.yaml"));
        String gate = Files.readString(CHART_DIR.resolve("templates/migration-gate-rbac.yaml"));
        String rollout = Files.readString(CHART_DIR.resolve("templates/rollout.yaml"));
        String runnerPom = Files.readString(REPOSITORY_ROOT.resolve("migration-runner/pom.xml"));
        String migrationDockerfile = Files.readString(REPOSITORY_ROOT.resolve("Dockerfile.migration"));
        String runtimeSecret = Files.readString(MANIFEST_DIR.resolve("external-secrets/runtime-secret.yml"));
        String localGrants = Files.readString(REPOSITORY_ROOT.resolve("ops/mysql/init/01-create-databases.sql"));
        String physicalTableTemplate =
                Files.readString(REPOSITORY_ROOT.resolve("ops/mysql/sharding/physical-table-template.sql"));

        assertThat(MANIFEST_DIR.resolve("migration-runner.yml")).doesNotExist();
        assertThat(runnerPom).doesNotContain("maven.multiModuleProjectDirectory")
                .doesNotContain("targetPath>migrations");
        assertThat(migrationDockerfile).contains("ARG MODULE")
                .contains("COPY ${MODULE}/src/main/resources/db/migration/ /app/migrations/");
        assertThat(job).contains("kind: Job").contains("backoffLimit:").contains("activeDeadlineSeconds:")
                .contains("restartPolicy: Never").contains("{{ .name }}-migration").contains("{{ .name }}-migration:")
                .contains("EMALL_MIGRATION_SERVICE").contains("EMALL_MIGRATION_CANARY_SHARD_COUNT")
                .contains("EMALL_MIGRATION_BATCH_SIZE").contains("EMALL_MIGRATION_BATCH_TIMEOUT")
                .contains("EMALL_MIGRATION_PHASE").doesNotContain("emall-runtime-secret");
        assertThat(secret).contains("kind: ExternalSecret").contains("remoteKeyPrefix").contains("/{{ .name }}")
                .contains("EMALL_MIGRATION_DB_USERNAME").contains("EMALL_MIGRATION_DB_PASSWORD");
        assertThat(runtimeSecret).doesNotContain("EMALL_MIGRATION_DB_USERNAME")
                .doesNotContain("EMALL_MIGRATION_DB_PASSWORD").doesNotContain("key: emall/migration");
        assertThat(gate).contains("resourceNames:").contains("{{ .name }}-migration-gate").contains("- get")
                .contains("- watch").doesNotContain("- list");
        assertThat(rollout).contains("wait-for-schema-migration").contains("kubectl")
                .contains("job/{{ printf \"%s-migration-%s\"").contains("serviceAccountToken:");
        for (String service : MIGRATION_RUNNER_SERVICES) {
            assertThat(REPOSITORY_ROOT.resolve(service).resolve("src/main/resources/db/migration"))
                    .as("migration directory for %s", service).isDirectory();
            String principal = service.replace('-', '_') + "_migration";
            String database = "emall_" + service.replace('-', '_');
            assertThat(localGrants).contains("CREATE USER IF NOT EXISTS '" + principal + "'@'%'")
                    .contains("GRANT ALL PRIVILEGES ON " + database + ".* TO '" + principal + "'@'%'");
        }
        assertThat(physicalTableTemplate).contains("CREATE TABLE {table}_{index} LIKE {source_table}")
                .contains("CHECK (shard_id = {shard_id})").contains("CHECK (cell_id = '{cell_id}')");
    }

    @Test
    void shouldWireLeastPrivilegeProductionControlPlanesThroughHelm() throws IOException {
        String values = Files.readString(CHART_DIR.resolve("values.yaml"));
        String serviceAccounts = Files.readString(CHART_DIR.resolve("templates/serviceaccount.yaml"));
        String rbac = Files.readString(CHART_DIR.resolve("templates/control-plane-rbac.yaml"));
        String runtimeSecret = Files.readString(MANIFEST_DIR.resolve("external-secrets/runtime-secret.yml"));

        assertThat(values).contains("name: release").contains("name: reliability").contains("kubernetesApiAccess: true")
                .contains("- argoproj.io").contains("- rollouts").contains("- monitoring.coreos.com")
                .contains("- prometheusrules").contains("EMALL_CONTROL_PLANE_NACOS_URL")
                .contains("EMALL_INFRASTRUCTURE_OPERATOR_URL");
        assertThat(serviceAccounts).contains("automountServiceAccountToken:").contains(".kubernetesApiAccess");
        assertThat(rbac).contains("kind: Role").contains("kind: RoleBinding").contains(".rbacRules");
        assertThat(runtimeSecret).contains("EMALL_CONTROL_PLANE_NACOS_ACCESS_TOKEN")
                .contains("EMALL_INFRASTRUCTURE_OPERATOR_TOKEN");
    }

    @Test
    void shouldGateChartRenderingSchemaAndServerDryRunInCi() throws IOException {
        String workflow = Files.readString(REPOSITORY_ROOT.resolve(".github/workflows/ci.yml"));

        assertThat(workflow).contains("deployment-manifests:").contains("helm lint ops/helm/emall --strict")
                .contains("helm template emall ops/helm/emall").contains("kubeconform-linux-amd64.tar.gz")
                .contains("sha256sum --check").contains("-strict -summary -ignore-missing-schemas")
                .contains("argoproj/argo-rollouts/releases/download/v1.8.3/install.yaml")
                .contains("migration.externalSecrets.enabled=false")
                .contains("kubectl apply --server-side --dry-run=server");
    }

    @Test
    void shouldRenderOneCompleteAndUniqueProductionManifestWithHelm() throws Exception {
        Path helm = helmExecutable();
        if (helm == null) {
            if (Boolean.parseBoolean(System.getenv("EMALL_INTEGRATION_REQUIRE_HELM"))) {
                fail("Helm is required but no executable was found");
            }
            Assumptions.abort("Helm executable is not available");
        }

        Path output = Files.createTempFile(Path.of("target"), "helm-rendered-", ".yaml");
        String rendered;
        try {
            Process process = new ProcessBuilder(helm.toString(), "template", "emall", CHART_DIR.toString(),
                    "--namespace", "emall").redirectErrorStream(true).redirectOutput(output.toFile()).start();
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                fail("Helm rendering timed out");
            }
            rendered = Files.readString(output, StandardCharsets.UTF_8);
            assertThat(process.exitValue()).as(rendered).isZero();
            assertThat(rendered).doesNotContain(":latest").doesNotContain("kind: Deployment");
        } finally {
            Files.deleteIfExists(output);
        }

        Map<String, Integer> kindCounts = new LinkedHashMap<>();
        Set<String> resourceKeys = new HashSet<>();
        Set<String> rolloutNames = new HashSet<>();
        Set<String> migrationJobNames = new HashSet<>();
        for (Object value : yaml().loadAll(rendered)) {
            if (!(value instanceof Map<?, ?> resource)) {
                continue;
            }
            String apiVersion = String.valueOf(resource.get("apiVersion"));
            String kind = String.valueOf(resource.get("kind"));
            Map<?, ?> metadata = (Map<?, ?>) resource.get("metadata");
            String name = String.valueOf(metadata.get("name"));
            kindCounts.merge(kind, 1, Integer::sum);
            assertThat(resourceKeys.add(apiVersion + '/' + kind + '/' + name))
                    .as("duplicate rendered resource %s/%s/%s", apiVersion, kind, name).isTrue();
            if (kind.equals("Rollout")) {
                rolloutNames.add(name);
            }
            if (kind.equals("Job") && name.contains("-migration-")) {
                migrationJobNames.add(name);
            }
        }

        assertThat(rolloutNames).containsExactlyInAnyOrderElementsOf(ONLINE_SERVICES);
        assertThat(migrationJobNames).hasSize(MIGRATION_RUNNER_SERVICES.size());
        for (String service : MIGRATION_RUNNER_SERVICES) {
            assertThat(migrationJobNames).contains(service + "-migration-0-1-0");
        }
        assertThat(kindCounts).containsEntry("Rollout", ONLINE_SERVICES.size())
                .containsEntry("Service", ONLINE_SERVICES.size())
                .containsEntry("HorizontalPodAutoscaler", ONLINE_SERVICES.size())
                .containsEntry("PodDisruptionBudget", ONLINE_SERVICES.size())
                .containsEntry("ServiceAccount", ONLINE_SERVICES.size() + MIGRATION_RUNNER_SERVICES.size())
                .containsEntry("Job", MIGRATION_RUNNER_SERVICES.size())
                .containsEntry("ExternalSecret", MIGRATION_RUNNER_SERVICES.size())
                .containsEntry("Role", 2 + MIGRATION_RUNNER_SERVICES.size())
                .containsEntry("RoleBinding", 2 + MIGRATION_RUNNER_SERVICES.size()).containsEntry("Gateway", 1)
                .containsEntry("HTTPRoute", 2).containsEntry("AnalysisTemplate", 5);
    }

    private Path helmExecutable() {
        String configured = System.getenv("EMALL_HELM_EXECUTABLE");
        if (configured != null && !configured.isBlank() && Files.isRegularFile(Path.of(configured))) {
            return Path.of(configured);
        }
        Path local = REPOSITORY_ROOT.resolve("target/tools/helm/windows-amd64/helm.exe");
        if (Files.isRegularFile(local)) {
            return local;
        }
        for (String directory : System.getenv().getOrDefault("PATH", "").split(java.io.File.pathSeparator)) {
            Path candidate = Path.of(directory, isWindows() ? "helm.exe" : "helm");
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private void assertDirectoryIsAbsentOrEmpty(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var files = Files.list(directory)) {
            assertThat(files).as("obsolete raw manifest directory %s", directory).isEmpty();
        }
    }

    private Yaml yaml() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> yamlMap(String value) {
        return (Map<String, Object>) yaml().load(value);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
