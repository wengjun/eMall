package com.emall.common;

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
    void shouldContainDubboRpcContractsForCoreTransactionChain() throws IOException {
        Path moduleRoot = Path.of("").toAbsolutePath();
        Path rpcRoot = moduleRoot.resolve("src/main/java/com/emall/common/rpc");

        assertThat(rpcRoot).isDirectory();
        assertThat(Files.readString(rpcRoot.resolve("InventoryRpcService.java")))
                .contains("interface InventoryRpcService");
        assertThat(Files.readString(rpcRoot.resolve("PricingRpcService.java"))).contains("interface PricingRpcService");
        assertThat(Files.readString(rpcRoot.resolve("MarketingRpcService.java")))
                .contains("interface MarketingRpcService");
        assertThat(Files.readString(rpcRoot.resolve("OrderRpcService.java"))).contains("interface OrderRpcService");
    }
}
