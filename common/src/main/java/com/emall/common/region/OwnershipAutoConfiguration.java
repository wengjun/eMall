package com.emall.common.region;

import com.emall.common.sharding.ShardRoutingProperties;
import com.emall.common.sharding.VirtualShardPlacementProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(OwnershipProperties.class)
public class OwnershipAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public OwnershipGuard ownershipGuard(OwnershipProperties properties,
            ObjectProvider<ShardRoutingProperties> shardRoutingProperties,
            ObjectProvider<VirtualShardPlacementProvider> placementProvider) {
        return new OwnershipGuard(properties, shardRoutingProperties.getIfAvailable(),
                placementProvider.getIfAvailable());
    }
}
