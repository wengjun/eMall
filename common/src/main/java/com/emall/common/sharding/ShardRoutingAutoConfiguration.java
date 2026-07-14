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
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    @ConditionalOnMissingBean
    public ShardRouteIndex shardRouteIndex(ShardRoutingProperties properties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        return new ShardRouteIndex(redisTemplateProvider.getIfAvailable(), properties.isEnabled());
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
    public DataSource routedDataSource(ShardDataSourceProperties properties, ShardRoutingProperties routingProperties) {
        Map<String, ShardDataSourceProperties.DataSourceSpec> configured =
                new LinkedHashMap<>(properties.getDatasources());
        if (configured.isEmpty() && StringUtils.hasText(properties.getJdbcUrlTemplate())) {
            for (int index = 0; index < routingProperties.getDatabaseShardCount(); index++) {
                String databaseName = "%s_%02d".formatted(routingProperties.getDatabasePrefix(), index);
                ShardDataSourceProperties.DataSourceSpec spec = new ShardDataSourceProperties.DataSourceSpec();
                spec.setJdbcUrl(properties.getJdbcUrlTemplate().replace("{database}", databaseName));
                spec.setUsername(properties.getUsername());
                spec.setPassword(properties.getPassword());
                configured.put(databaseName, spec);
            }
        }
        if (configured.isEmpty()) {
            throw new IllegalStateException("shard datasources or a shard JDBC URL template must be configured");
        }
        Map<Object, Object> targetDataSources = new LinkedHashMap<>();
        configured.forEach((name, spec) -> targetDataSources.put(name, hikari(name, spec)));
        String defaultName = StringUtils.hasText(properties.getDefaultName())
                ? properties.getDefaultName()
                : configured.keySet().iterator().next();
        Object defaultDataSource = targetDataSources.get(defaultName);
        if (defaultDataSource == null) {
            throw new IllegalStateException("default shard datasource is not configured: " + defaultName);
        }
        RoutedDataSource routedDataSource = new RoutedDataSource();
        routedDataSource.setTargetDataSources(targetDataSources);
        routedDataSource.setDefaultTargetDataSource(defaultDataSource);
        routedDataSource.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(routedDataSource);
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
