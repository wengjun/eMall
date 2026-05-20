package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ServiceConfigurationContractIT {
    private static final Path REPOSITORY_ROOT = Path.of("..").normalize();

    @Test
    void shouldKeepAllServiceConfigurationsProductionReady() throws IOException {
        for (Path applicationFile : applicationFiles()) {
            String moduleName =
                    applicationFile.getParent().getParent().getParent().getParent().getFileName().toString();
            String application = Files.readString(applicationFile);

            assertThat(application).as("%s application name", moduleName).contains("name: " + moduleName);
            if ("migration-runner".equals(moduleName)) {
                assertThat(application).contains("web-application-type: none").contains("emall:")
                        .contains("migration:");
                continue;
            }
            assertThat(application).as("%s service discovery and protection", moduleName).contains("nacos:")
                    .contains("sentinel:").contains("EMALL_NACOS_DISCOVERY_ENABLED").contains("EMALL_SENTINEL_ENABLED");
            assertThat(application).as("%s runtime observability", moduleName).contains("management:")
                    .contains("health:").contains("probes:").contains("prometheus");
            if (application.contains("datasource:")) {
                assertThat(application).as("%s persistence contract", moduleName).contains("storage: jdbc")
                        .contains("flyway:").contains("mybatis-plus:").contains("map-underscore-to-camel-case: true")
                        .contains("banner: false");
            }
        }
    }

    private List<Path> applicationFiles() throws IOException {
        try (Stream<Path> modules = Files.list(REPOSITORY_ROOT)) {
            return modules.map(path -> path.resolve("src/main/resources/application.yml")).filter(Files::isRegularFile)
                    .sorted().toList();
        }
    }
}
