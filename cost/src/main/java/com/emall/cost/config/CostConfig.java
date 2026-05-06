package com.emall.cost.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CostConfig {
    @Bean
    SnowflakeIdGenerator costIdGenerator() {
        return new SnowflakeIdGenerator(141L);
    }
}
