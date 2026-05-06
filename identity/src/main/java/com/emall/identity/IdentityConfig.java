package com.emall.identity;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IdentityConfig {
    @Bean
    SnowflakeIdGenerator identityIdGenerator() {
        return new SnowflakeIdGenerator(151L);
    }
}
