package com.emall.pricing.service;

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
    void shouldExposePricingThroughNacosAndDubbo() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        String pom = Files.readString(moduleRoot.resolve("pom.xml"));
        String application = Files.readString(moduleRoot.resolve("src/main/resources/application.yml"));
        String dubboProvider =
                Files.readString(moduleRoot.resolve("src/main/java/com/emall/pricing/rpc/PricingDubboService.java"));

        assertThat(pom).contains("spring-cloud-starter-alibaba-nacos-discovery")
                .contains("spring-cloud-starter-alibaba-nacos-config").contains("dubbo-spring-boot-starter");
        assertThat(application).contains("nacos:").contains("rpc:")
                .contains("address: ${EMALL_DUBBO_REGISTRY_ADDRESS:N/A}");
        assertThat(dubboProvider).contains("@DubboService").contains("PricingRpcService");
    }
}
