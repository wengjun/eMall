package com.emall.common.olap;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@AutoConfiguration
@EnableConfigurationProperties(ClickHouseProperties.class)
@ConditionalOnProperty(name = "emall.olap.engine", havingValue = "clickhouse")
public class ClickHouseAutoConfiguration {
    public static final String CLICKHOUSE_DATA_SOURCE_BEAN = "clickHouseDataSource";
    public static final String CLICKHOUSE_JDBC_TEMPLATE_BEAN = "clickHouseJdbcTemplate";

    @Bean(CLICKHOUSE_DATA_SOURCE_BEAN)
    @ConditionalOnMissingBean(name = CLICKHOUSE_DATA_SOURCE_BEAN)
    public DataSource clickHouseDataSource(ClickHouseProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.getClickhouseUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return dataSource;
    }

    @Bean(CLICKHOUSE_JDBC_TEMPLATE_BEAN)
    @ConditionalOnMissingBean(name = CLICKHOUSE_JDBC_TEMPLATE_BEAN)
    public JdbcTemplate clickHouseJdbcTemplate(
            @Qualifier(CLICKHOUSE_DATA_SOURCE_BEAN) DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }
}
