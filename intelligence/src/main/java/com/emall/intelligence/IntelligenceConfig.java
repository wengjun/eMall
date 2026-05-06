package com.emall.intelligence;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IntelligenceConfig {
    @Bean
    SnowflakeIdGenerator intelligenceIdGenerator() {
        return new SnowflakeIdGenerator(193L);
    }
}
