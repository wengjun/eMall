package com.emall.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MigrationAssetsTest {
    private static final Path REPOSITORY_ROOT = Path.of("..").normalize();

    @Test
    void keepsEachServiceMigrationArtifactIndependent() throws IOException {
        String runnerPom = Files.readString(Path.of("pom.xml"));
        String dockerfile = Files.readString(REPOSITORY_ROOT.resolve("Dockerfile.migration"));

        assertThat(runnerPom).doesNotContain("maven.multiModuleProjectDirectory")
                .doesNotContain("targetPath>migrations");
        assertThat(dockerfile).contains("ARG MODULE")
                .contains("COPY ${MODULE}/src/main/resources/db/migration/ /app/migrations/")
                .doesNotContain("COPY */src/main/resources/db/migration");
        for (String service : MigrationRunnerProperties.SUPPORTED_SERVICES) {
            Path directory = REPOSITORY_ROOT.resolve(service).resolve("src/main/resources/db/migration");
            assertThat(directory).as("migration directory for %s", service).isDirectory();
            try (var scripts = Files.list(directory)) {
                assertThat(scripts.filter(path -> path.toString().endsWith(".sql")))
                        .as("migration scripts for %s", service).isNotEmpty();
            }
        }
    }

    @Test
    void supportedServiceInventoryMatchesDatabaseModules() {
        assertThat(MigrationRunnerProperties.SUPPORTED_SERVICES).hasSize(37).doesNotHaveDuplicates().contains("order",
                "payment", "inventory", "identity", "event-platform", "platform-ops", "routing");
    }
}
