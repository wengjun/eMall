package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class LoadTestHelmChartIT {
    private static final Path REPOSITORY_ROOT = Path.of("..").normalize();
    private static final Path CHART = REPOSITORY_ROOT.resolve("ops/loadtest");

    @Test
    void shouldRenderDistributedWorkerAndCoordinatorJobs() throws Exception {
        Path helm = helmExecutable();
        if (helm == null) {
            if (Boolean.parseBoolean(System.getenv("EMALL_INTEGRATION_REQUIRE_HELM"))) {
                fail("Helm is required but no executable was found");
            }
            Assumptions.abort("Helm executable is not available");
        }

        String worker = render(helm, "worker");
        String coordinator = render(helm, "coordinator");

        assertThat(worker).contains("completionMode: Indexed").contains("EMALL_LOAD_WORKER_INDEX")
                .contains("batch.kubernetes.io/job-completion-index").contains("topologySpreadConstraints:")
                .contains("readOnlyRootFilesystem: true").contains("accessModes:").contains("ReadWriteMany");
        assertThat(coordinator).contains("EMALL_LOAD_EVIDENCE_RUN_IDS").contains("EMALL_LOAD_REQUIRE_VERIFIED_EVIDENCE")
                .contains("value: coordinator");
    }

    private String render(Path helm, String mode) throws IOException, InterruptedException {
        Path output = Files.createTempFile(Path.of("target"), "loadtest-helm-", ".yaml");
        try {
            Process process = new ProcessBuilder(helm.toString(), "template", "capacity", CHART.toString(), "--set",
                    "mode=" + mode).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("Helm rendering timed out");
            }
            String rendered = Files.readString(output, StandardCharsets.UTF_8);
            assertThat(process.exitValue()).as(rendered).isZero();
            return rendered;
        } finally {
            Files.deleteIfExists(output);
        }
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
}
