package com.emall.common.id;

import java.lang.management.ManagementFactory;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(SnowflakeIdProperties.class)
public class SnowflakeIdAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(WorkerIdLease.class)
    WorkerIdLease workerIdLease(SnowflakeIdProperties properties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${POD_UID:${HOSTNAME:local}}") String instanceName) {
        if (!properties.isLeaseEnabled()) {
            long workerId = properties.getWorkerId() == null
                    ? Math.floorMod(serviceName.hashCode(), 1024)
                    : properties.getWorkerId();
            return WorkerIdLease.permanent(workerId);
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            throw new IllegalStateException("Redis is required when Snowflake worker leasing is enabled");
        }
        String runtimeId = ManagementFactory.getRuntimeMXBean().getName();
        String owner = serviceName + ':' + instanceName + ':' + runtimeId + ':' + UUID.randomUUID();
        return new RedisWorkerIdLease(redisTemplate, owner, properties);
    }

    @Bean
    @ConditionalOnMissingBean(SnowflakeIdGenerator.class)
    SnowflakeIdGenerator snowflakeIdGenerator(WorkerIdLease lease, SnowflakeIdProperties properties) {
        return new SnowflakeIdGenerator(lease, properties.getMaximumClockRollbackMillis());
    }
}
