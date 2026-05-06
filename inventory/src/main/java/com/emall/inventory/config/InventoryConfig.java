package com.emall.inventory.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryConfig {
    @Bean
    SnowflakeIdGenerator inventoryIdGenerator() {
        return new SnowflakeIdGenerator(31L);
    }
}
