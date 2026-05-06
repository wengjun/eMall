package com.emall.marketing.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketingConfig {
    @Bean
    SnowflakeIdGenerator couponIdGenerator() {
        return new SnowflakeIdGenerator(81L);
    }
}
