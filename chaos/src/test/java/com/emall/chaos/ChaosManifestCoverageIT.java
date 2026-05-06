package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ChaosManifestCoverageIT {
    private static final Path MANIFEST_DIR = Path.of("..", "ops", "k8s", "chaos").normalize();

    @Test
    void shouldHaveKubernetesManifestForEveryBaselineDrill() throws IOException {
        for (ChaosDrill drill : ChaosDrillCatalog.p3Baseline()) {
            Path manifest = MANIFEST_DIR.resolve(drill.code() + ".yml");
            assertThat(manifest)
                    .as("manifest for %s", drill.code())
                    .exists()
                    .isRegularFile();

            String content = Files.readString(manifest);
            assertThat(content)
                    .as("manifest content for %s", drill.code())
                    .contains("apiVersion: chaos-mesh.org/v1alpha1")
                    .contains("duration: " + drill.duration().toMinutes() + "m");
        }
    }
}
