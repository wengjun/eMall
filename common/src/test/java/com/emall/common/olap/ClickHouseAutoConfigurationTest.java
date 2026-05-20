package com.emall.common.olap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class ClickHouseAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ClickHouseAutoConfiguration.class));

    @Test
    void shouldCreateClickHouseJdbcTemplateWhenEnabled() {
        contextRunner
                .withPropertyValues("emall.olap.engine=clickhouse",
                        "emall.olap.clickhouse-url=jdbc:clickhouse://clickhouse:8123/emall")
                .run(context -> assertThat(context).hasBean(ClickHouseAutoConfiguration.CLICKHOUSE_JDBC_TEMPLATE_BEAN)
                        .getBean(ClickHouseAutoConfiguration.CLICKHOUSE_JDBC_TEMPLATE_BEAN, JdbcTemplate.class)
                        .isInstanceOf(JdbcTemplate.class));
    }

    @Test
    void shouldStayDisabledByDefault() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(ClickHouseAutoConfiguration.CLICKHOUSE_JDBC_TEMPLATE_BEAN));
    }
}
