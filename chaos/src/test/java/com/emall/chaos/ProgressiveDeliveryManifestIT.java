package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProgressiveDeliveryManifestIT {
    private static final Path ROLLOUT_DIR = Path.of("..", "ops", "k8s", "rollouts").normalize();

    @Test
    void shouldGateCanaryAndBlueGreenRolloutsWithSloAnalysis() throws IOException {
        String orderCanary = Files.readString(ROLLOUT_DIR.resolve("order-canary.yml"));
        String paymentBlueGreen = Files.readString(ROLLOUT_DIR.resolve("payment-blue-green.yml"));
        String sloTemplates = Files.readString(ROLLOUT_DIR.resolve("slo-analysis-templates.yml"));

        assertThat(orderCanary)
                .contains("kind: Rollout")
                .contains("canary:")
                .contains("setWeight: 5")
                .contains("setWeight: 20")
                .contains("setWeight: 50")
                .contains("templateName: emall-http-slo")
                .contains("abortScaleDownDelaySeconds: 30");
        assertThat(paymentBlueGreen)
                .contains("blueGreen:")
                .contains("autoPromotionEnabled: false")
                .contains("prePromotionAnalysis:")
                .contains("templateName: payment-preview-smoke")
                .contains("templateName: emall-payment-slo");
        assertThat(sloTemplates)
                .contains("name: emall-http-slo")
                .contains("name: emall-payment-slo")
                .contains("provider:")
                .contains("prometheus:")
                .contains("successCondition:");
    }
}
