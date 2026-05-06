package com.emall.platformops;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PlatformOpsConfig {
    @Bean
    SnowflakeIdGenerator platformOpsIdGenerator() {
        return new SnowflakeIdGenerator(204L);
    }
}
