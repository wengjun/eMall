package com.emall.product.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ProductConfig {
    @Bean
    SnowflakeIdGenerator productIdGenerator() {
        return new SnowflakeIdGenerator(21L);
    }
}
