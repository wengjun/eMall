package com.emall.common.region;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(OwnershipProperties.class)
public class OwnershipAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public OwnershipGuard ownershipGuard(OwnershipProperties properties) {
        return new OwnershipGuard(properties);
    }
}
