package com.emall.order.service;

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
    void shouldUseSentinelInsteadOfResilience4jForDownstreamProtection() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        String pom = Files.readString(moduleRoot.resolve("pom.xml"));
        String application = Files.readString(moduleRoot.resolve("src/main/resources/application.yml"));
        String inventoryClient =
                Files.readString(moduleRoot.resolve("src/main/java/com/emall/order/integration/InventoryClient.java"));
        String ruleConfig = Files.readString(
                moduleRoot.resolve("src/main/java/com/emall/order/config/OrderSentinelRuleConfiguration.java"));

        assertThat(pom).contains("spring-cloud-starter-alibaba-sentinel").doesNotContain("resilience4j");
        assertThat(application).contains("sentinel:").doesNotContain("resilience4j");
        assertThat(inventoryClient).contains("@SentinelResource").contains("order.inventory.reserve");
        assertThat(ruleConfig).contains("FlowRuleManager").contains("DegradeRuleManager")
                .contains("order.pricing.quote");
    }

    @Test
    void shouldPreferDubboRpcWhenDomesticRuntimeIsEnabled() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        String pom = Files.readString(moduleRoot.resolve("pom.xml"));
        String application = Files.readString(moduleRoot.resolve("src/main/resources/application.yml"));
        String inventoryClient =
                Files.readString(moduleRoot.resolve("src/main/java/com/emall/order/integration/InventoryClient.java"));
        String pricingClient =
                Files.readString(moduleRoot.resolve("src/main/java/com/emall/order/integration/PricingClient.java"));
        String marketingClient =
                Files.readString(moduleRoot.resolve("src/main/java/com/emall/order/integration/MarketingClient.java"));
        String dubboProvider =
                Files.readString(moduleRoot.resolve("src/main/java/com/emall/order/rpc/OrderDubboService.java"));

        assertThat(pom).contains("spring-cloud-starter-alibaba-nacos-discovery")
                .contains("spring-cloud-starter-alibaba-nacos-config").contains("dubbo-spring-boot-starter");
        assertThat(application).contains("nacos:").contains("emall:").contains("rpc:")
                .contains("address: ${EMALL_DUBBO_REGISTRY_ADDRESS:N/A}");
        assertThat(inventoryClient).contains("@DubboReference").contains("InventoryRpcService")
                .contains("dubboEnabled()");
        assertThat(pricingClient).contains("@DubboReference").contains("PricingRpcService");
        assertThat(marketingClient).contains("@DubboReference").contains("MarketingRpcService");
        assertThat(dubboProvider).contains("@DubboService").contains("OrderRpcService");
    }
}
