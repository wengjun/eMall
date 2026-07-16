package com.emall.common.sharding;

import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.emall.common.metrics.BusinessMetrics;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties({ShardRoutingProperties.class, ShardDataSourceProperties.class,
        ShardRouteDirectoryProperties.class})
public class ShardRoutingAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public VirtualShardPlacementProvider virtualShardPlacementProvider(ShardRoutingProperties properties,
            ShardRouteDirectoryProperties directoryProperties, ObjectProvider<RestClient.Builder> builderProvider,
            Environment environment) {
        if (!StringUtils.hasText(properties.getMappingNamespace())) {
            properties.setMappingNamespace(environment.getProperty("spring.application.name",
                    properties.getDatabasePrefix().replace('_', '-')));
        }
        VirtualShardPlacementProvider fallback = new StaticVirtualShardPlacementProvider(properties);
        if (!StringUtils.hasText(directoryProperties.getEndpoint())) {
            return fallback;
        }
        RestClient.Builder builder = builderProvider.getIfAvailable();
        if (builder == null) {
            throw new IllegalStateException("RestClient is required for the virtual shard placement control plane");
        }
        String internalKey = StringUtils.hasText(directoryProperties.getInternalOperationKey())
                ? directoryProperties.getInternalOperationKey()
                : environment.getProperty("emall.internal.operations-token", "");
        return new HttpVirtualShardPlacementProvider(builder, directoryProperties.getEndpoint(), internalKey, fallback,
                properties.getMappingCacheTtl(), properties.getStaleReadTtl(), properties.getVirtualShardCount());
    }

    @Bean
    @ConditionalOnMissingBean
    public ShardRoutingOperations shardRoutingOperations(ShardRoutingProperties properties,
            VirtualShardPlacementProvider placementProvider) {
        return new DefaultShardRoutingOperations(properties, placementProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public ShardRouteDirectory shardRouteDirectory(ShardRouteDirectoryProperties properties,
            ObjectProvider<RestClient.Builder> builderProvider, Environment environment) {
        if (StringUtils.hasText(properties.getEndpoint())) {
            RestClient.Builder builder = builderProvider.getIfAvailable();
            if (builder == null) {
                throw new IllegalStateException("RestClient is required for the persistent shard route directory");
            }
            String internalKey = StringUtils.hasText(properties.getInternalOperationKey())
                    ? properties.getInternalOperationKey()
                    : environment.getProperty("emall.internal.operations-token", "");
            return new HttpShardRouteDirectory(builder, properties.getEndpoint(), internalKey);
        }
        return new InMemoryShardRouteDirectory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ShardRouteIndex shardRouteIndex(ShardRoutingProperties properties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider, ShardRouteDirectory directory,
            ShardRouteDirectoryProperties directoryProperties, ObjectProvider<BusinessMetrics> metricsProvider) {
        return new ShardRouteIndex(redisTemplateProvider.getIfAvailable(), properties.isEnabled(), directory,
                directoryProperties, Clock.systemUTC(), metricsProvider.getIfAvailable(BusinessMetrics::noop));
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
    public DataSource routedDataSource(ShardDataSourceProperties properties, ShardRoutingProperties routingProperties,
            ObjectProvider<MeterRegistry> meterRegistryProvider) {
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
        Map<String, ShardConnectionBudget.PoolAllocation> allocations =
                ShardConnectionBudget.plan(properties, configured);
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        Map<Object, Object> targetDataSources = new LinkedHashMap<>();
        ArrayList<LazyShardDataSource> managedDataSources = new ArrayList<>(configured.size());
        configured.forEach((name, spec) -> {
            LazyShardDataSource dataSource =
                    new LazyShardDataSource(() -> hikari(name, spec, allocations.get(name), properties, meterRegistry));
            if (!properties.isLazyInitialization()) {
                initialize(dataSource, name);
            }
            targetDataSources.put(name, dataSource);
            managedDataSources.add(dataSource);
        });
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
        routedDataSource.setLenientFallback(false);
        routedDataSource.manage(managedDataSources);
        routedDataSource.afterPropertiesSet();
        ShardConnectionPoolMetrics.bind(meterRegistry, managedDataSources, allocations);
        return new ManagedLazyRoutingDataSource(routedDataSource);
    }

    private HikariDataSource hikari(String name, ShardDataSourceProperties.DataSourceSpec spec,
            ShardConnectionBudget.PoolAllocation allocation, ShardDataSourceProperties properties,
            MeterRegistry meterRegistry) {
        if (!StringUtils.hasText(spec.getJdbcUrl())) {
            throw new IllegalStateException("jdbcUrl is required for shard datasource: " + name);
        }
        HikariConfig config = new HikariConfig();
        config.setPoolName("emall-shard-" + name);
        config.setJdbcUrl(spec.getJdbcUrl());
        config.setUsername(spec.getUsername());
        config.setPassword(spec.getPassword());
        config.setMaximumPoolSize(allocation.maximumPoolSize());
        config.setMinimumIdle(allocation.minimumIdle());
        config.setConnectionTimeout(spec.getConnectionTimeoutMs());
        config.setValidationTimeout(spec.getValidationTimeoutMs());
        config.setIdleTimeout(properties.getIdleTimeoutMs());
        config.setMaxLifetime(properties.getMaxLifetimeMs());
        config.setInitializationFailTimeout(-1);
        if (meterRegistry != null) {
            config.setMetricRegistry(meterRegistry);
        }
        return new HikariDataSource(config);
    }

    private void initialize(LazyShardDataSource dataSource, String name) {
        try {
            dataSource.initialize();
        } catch (SQLException exception) {
            throw new IllegalStateException("failed to initialize shard datasource: " + name, exception);
        }
    }
}
