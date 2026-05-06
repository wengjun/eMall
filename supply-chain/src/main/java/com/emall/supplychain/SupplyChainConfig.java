package com.emall.supplychain;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SupplyChainConfig {
    @Bean
    SnowflakeIdGenerator supplyChainIdGenerator() {
        return new SnowflakeIdGenerator(181L);
    }
}
