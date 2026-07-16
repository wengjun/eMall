package com.emall.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MessagingAutoConfigurationTest {
    @Test
    void shouldUseDatabaseAggregateVersionGuardWhenMapperIsAvailable() {
        new ApplicationContextRunner()
                .withBean(AggregateVersionRecordMapper.class, () -> mock(AggregateVersionRecordMapper.class))
                .withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration.class)).run(context -> {
                    assertThat(context).hasSingleBean(AggregateVersionGuard.class);
                    assertThat(context.getBean(AggregateVersionGuard.class))
                            .isInstanceOf(MybatisPlusAggregateVersionGuard.class);
                });
    }

    @Test
    void shouldUseInMemoryAggregateVersionGuardWithoutMybatisMapper() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MessagingAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(AggregateVersionGuard.class);
                    assertThat(context.getBean(AggregateVersionGuard.class))
                            .isInstanceOf(InMemoryAggregateVersionGuard.class);
                });
    }
}
