package com.emall.merchant.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MerchantConfig {
    @Bean
    SnowflakeIdGenerator merchantIdGenerator() {
        return new SnowflakeIdGenerator(111L);
    }
}
