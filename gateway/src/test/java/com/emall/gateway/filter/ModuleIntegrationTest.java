package com.emall.gateway.filter;

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
    void shouldUseNacosBackedLoadBalancedRoutes() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        String pom = Files.readString(moduleRoot.resolve("pom.xml"));
        String application = Files.readString(moduleRoot.resolve("src/main/resources/application.yml"));

        assertThat(pom).contains("spring-cloud-starter-loadbalancer")
                .contains("spring-cloud-starter-alibaba-nacos-discovery")
                .contains("spring-cloud-starter-alibaba-nacos-config");
        assertThat(application).contains("nacos:").contains("EMALL_NACOS_DISCOVERY_ENABLED")
                .contains("uri: ${EMALL_ORDER_URL:lb://order}").contains("uri: ${EMALL_PAYMENT_URL:lb://payment}");
    }

    @Test
    void shouldApplySecureHeadersAtGatewayEdge() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        String application = Files.readString(moduleRoot.resolve("src/main/resources/application.yml"));

        assertThat(application).contains("default-filters:").contains("- SecureHeaders")
                .contains("RequestRateLimiter");
    }
}
