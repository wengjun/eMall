package com.emall.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModuleIntegrationTest {
    @Test
    void shouldExposeModuleBuildInputs() {
        Path moduleRoot = Path.of("").toAbsolutePath();

        assertThat(moduleRoot.resolve("pom.xml")).isRegularFile();
        assertThat(moduleRoot.resolve("src/main/java")).isDirectory();
    }

    @Test
    void shouldDeclareElasticsearchSearchBackend() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        String pom = Files.readString(moduleRoot.resolve("pom.xml"));
        String application = Files.readString(moduleRoot.resolve("src/main/resources/application.yml"));

        assertThat(pom).contains("spring-boot-starter-data-elasticsearch");
        assertThat(application).contains("engine: elasticsearch").contains("EMALL_ELASTICSEARCH_URL")
                .contains("spring:").contains("elasticsearch:");
    }
}
