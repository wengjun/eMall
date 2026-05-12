package com.emall.payment.service;

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
    void shouldUseSentinelInsteadOfResilience4jForOrderCallbackProtection() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        String pom = Files.readString(moduleRoot.resolve("pom.xml"));
        String application = Files.readString(moduleRoot.resolve("src/main/resources/application.yml"));
        String orderClient =
                Files.readString(moduleRoot.resolve("src/main/java/com/emall/payment/integration/OrderClient.java"));
        String ruleConfig = Files.readString(
                moduleRoot.resolve("src/main/java/com/emall/payment/config/PaymentSentinelRuleConfiguration.java"));

        assertThat(pom).contains("spring-cloud-starter-alibaba-sentinel").doesNotContain("resilience4j");
        assertThat(application).contains("sentinel:").doesNotContain("resilience4j");
        assertThat(orderClient).contains("@SentinelResource").contains("payment.order.pay");
        assertThat(ruleConfig).contains("FlowRuleManager").contains("DegradeRuleManager");
    }

    @Test
    void shouldUseDubboForOrderCallbacksWhenDomesticRuntimeIsEnabled() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        String pom = Files.readString(moduleRoot.resolve("pom.xml"));
        String application = Files.readString(moduleRoot.resolve("src/main/resources/application.yml"));
        String orderClient =
                Files.readString(moduleRoot.resolve("src/main/java/com/emall/payment/integration/OrderClient.java"));

        assertThat(pom).contains("spring-cloud-starter-alibaba-nacos-discovery")
                .contains("spring-cloud-starter-alibaba-nacos-config").contains("dubbo-spring-boot-starter");
        assertThat(application).contains("nacos:").contains("rpc:")
                .contains("address: ${EMALL_DUBBO_REGISTRY_ADDRESS:N/A}");
        assertThat(orderClient).contains("@DubboReference").contains("OrderRpcService").contains("dubboEnabled()");
    }
}
