package com.emall.eventplatform;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EventPlatformConfig {
    @Bean
    SnowflakeIdGenerator eventPlatformIdGenerator() {
        return new SnowflakeIdGenerator(191L);
    }
}
