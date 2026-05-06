package com.emall.common.runtime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionRuntimeGuardTest {
    @Test
    void shouldRejectUnsafeProductionDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("emall.runtime.guard.enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:mysql://db/emall_order")
                .withProperty("spring.datasource.username", "emall")
                .withProperty("spring.datasource.password", "secret")
                .withProperty("emall.internal.operations-token", "local-dev-token");

        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("emall.internal.operations-token");
    }

    @Test
    void shouldAllowCompleteProductionConfiguration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("emall.runtime.guard.enabled", "true")
                .withProperty("spring.datasource.url", "jdbc:mysql://db/emall_order")
                .withProperty("spring.datasource.username", "emall")
                .withProperty("spring.datasource.password", "secret")
                .withProperty("emall.internal.operations-token", "prod-token");

        ProductionRuntimeGuard guard = new ProductionRuntimeGuard(environment, List.of());

        assertThatCode(() -> guard.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }
}
