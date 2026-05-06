package com.emall.analytics;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AnalyticsConfig {
    @Bean
    SnowflakeIdGenerator analyticsIdGenerator() {
        return new SnowflakeIdGenerator(194L);
    }
}
