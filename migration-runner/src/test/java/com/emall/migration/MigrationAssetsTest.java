package com.emall.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class MigrationAssetsTest {
    private final PathMatchingResourcePatternResolver resources = new PathMatchingResourcePatternResolver();

    @Test
    void packagesAtLeastOneVersionedMigrationForEverySupportedService() throws IOException {
        for (String service : MigrationRunnerProperties.SUPPORTED_SERVICES) {
            String location = "classpath*:migrations/" + service + "/src/main/resources/db/migration/*.sql";
            assertThat(resources.getResources(location)).as("migration scripts for %s", service).isNotEmpty();
        }
    }

    @Test
    void supportedServiceInventoryMatchesProductionDatabaseModules() {
        assertThat(MigrationRunnerProperties.SUPPORTED_SERVICES).hasSize(36).doesNotHaveDuplicates().contains("order",
                "payment", "inventory", "identity", "event-platform", "platform-ops");
    }
}
