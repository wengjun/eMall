package com.emall.common.sharding;

import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@AutoConfiguration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties({ShardRoutingProperties.class, ShardDataSourceProperties.class})
public class ShardRoutingAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ShardRoutingOperations shardRoutingOperations(ShardRoutingProperties properties) {
        return new DefaultShardRoutingOperations(properties);
    }

    @Bean
    @ConditionalOnClass(DynamicTableNameInnerInterceptor.class)
    @ConditionalOnProperty(prefix = "emall.sharding", name = "enabled", havingValue = "true")
    public DynamicTableNameInnerInterceptor shardDynamicTableNameInnerInterceptor() {
        DynamicTableNameInnerInterceptor interceptor = new DynamicTableNameInnerInterceptor();
        interceptor.setTableNameHandler((sql, tableName) -> ShardContext.resolveTableName(tableName));
        return interceptor;
    }

    @Bean
    @Primary
    @ConditionalOnClass(HikariDataSource.class)
    @ConditionalOnMissingBean(DataSource.class)
    @ConditionalOnProperty(prefix = "emall.sharding.datasource", name = "enabled", havingValue = "true")
    public DataSource routedDataSource(ShardDataSourceProperties properties) {
        if (properties.getDatasources().isEmpty()) {
            throw new IllegalStateException("emall.sharding.datasource.datasources must not be empty");
        }
        Map<Object, Object> targetDataSources = new LinkedHashMap<>();
        properties.getDatasources().forEach((name, spec) -> targetDataSources.put(name, hikari(name, spec)));
        String defaultName = StringUtils.hasText(properties.getDefaultName())
                ? properties.getDefaultName()
                : properties.getDatasources().keySet().iterator().next();
        Object defaultDataSource = targetDataSources.get(defaultName);
        if (defaultDataSource == null) {
            throw new IllegalStateException("default shard datasource is not configured: " + defaultName);
        }
        RoutedDataSource routedDataSource = new RoutedDataSource();
        routedDataSource.setTargetDataSources(targetDataSources);
        routedDataSource.setDefaultTargetDataSource(defaultDataSource);
        routedDataSource.afterPropertiesSet();
        return routedDataSource;
    }

    private HikariDataSource hikari(String name, ShardDataSourceProperties.DataSourceSpec spec) {
        if (!StringUtils.hasText(spec.getJdbcUrl())) {
            throw new IllegalStateException("jdbcUrl is required for shard datasource: " + name);
        }
        HikariConfig config = new HikariConfig();
        config.setPoolName("emall-shard-" + name);
        config.setJdbcUrl(spec.getJdbcUrl());
        config.setUsername(spec.getUsername());
        config.setPassword(spec.getPassword());
        config.setMaximumPoolSize(spec.getMaximumPoolSize());
        config.setConnectionTimeout(spec.getConnectionTimeoutMs());
        config.setValidationTimeout(spec.getValidationTimeoutMs());
        return new HikariDataSource(config);
    }
}
