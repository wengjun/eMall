package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProgressiveDeliveryManifestIT {
    private static final Path CHART_DIR = Path.of("..", "ops", "helm", "emall").normalize();

    @Test
    void shouldGateCanaryRolloutsWithSloAnalysis() throws IOException {
        String rollout = Files.readString(CHART_DIR.resolve("templates/rollout.yaml"));
        String sloTemplates = Files.readString(CHART_DIR.resolve("templates/analysis-templates.yaml"));

        assertThat(rollout).contains("kind: Rollout").contains("progressDeadlineAbort: true").contains("canary:")
                .contains("maxUnavailable:").contains("setWeight: 5").contains("setWeight: 25")
                .contains("setWeight: 50").contains("setWeight: 100").contains("templateName: emall-http-slo")
                .doesNotContain("blueGreen:").doesNotContain("stableIngress").doesNotContain("nginx:")
                .doesNotContain("kind: Deployment");
        assertThat(sloTemplates).contains("name: emall-http-slo").contains("name: emall-payment-slo")
                .contains("name: emall-release-pretraffic-guard").contains("name: emall-release-canary-guard")
                .contains("name: emall-release-rollback-recovery-guard").contains("provider:").contains("prometheus:")
                .contains("successCondition:").contains("failureCondition:");
    }
}
