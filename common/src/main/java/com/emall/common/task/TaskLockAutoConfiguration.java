package com.emall.common.task;

import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.common.metrics.BusinessMetrics;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class TaskLockAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(DistributedTaskLock.class)
    public DistributedTaskLock inMemoryDistributedTaskLock(Clock clock) {
        return new InMemoryDistributedTaskLock(clock, ownerId());
    }

    @Bean
    @ConditionalOnMissingBean
    public PartitionedShardWorkCoordinator partitionedShardWorkCoordinator(
            ObjectProvider<ShardRoutingOperations> shardRoutingOperations, DistributedTaskLock taskLock,
            ObjectProvider<BusinessMetrics> businessMetrics) {
        return new PartitionedShardWorkCoordinator(shardRoutingOperations.getIfAvailable(ShardRoutingOperations::noop),
                taskLock, businessMetrics.getIfAvailable(BusinessMetrics::noop));
    }

    private String ownerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException ex) {
            return "unknown-" + UUID.randomUUID();
        }
    }
}
