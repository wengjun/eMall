package com.emall.common.controlplane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ControlPlaneAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ControlPlaneAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new).withPropertyValues("emall.control-plane.enabled=true");

    @Test
    void selectsInMemoryStoreOnlyForExplicitMemoryMode() {
        contextRunner.withPropertyValues("emall.storage=memory").run(context -> {
            assertThat(context).hasSingleBean(ControlPlaneOperationStore.class).hasSingleBean(ControlPlaneClient.class);
            assertThat(context.getBean(ControlPlaneOperationStore.class))
                    .isInstanceOf(InMemoryControlPlaneOperationStore.class);
        });
    }

    @Test
    void selectsMybatisStoreForJdbcMode() {
        contextRunner.withPropertyValues("emall.storage=jdbc")
                .withBean(ControlPlaneOperationMapper.class, () -> mock(ControlPlaneOperationMapper.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ControlPlaneOperationStore.class);
                    assertThat(context.getBean(ControlPlaneOperationStore.class))
                            .isInstanceOf(MybatisPlusControlPlaneOperationStore.class);
                });
    }
}
