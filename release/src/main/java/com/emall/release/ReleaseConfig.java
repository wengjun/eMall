package com.emall.release;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ReleaseConfig {
    @Bean
    SnowflakeIdGenerator releaseIdGenerator() {
        return new SnowflakeIdGenerator(203L);
    }
}
