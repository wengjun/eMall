package com.emall.user.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserConfig {
    @Bean
    SnowflakeIdGenerator userIdGenerator() {
        return new SnowflakeIdGenerator(11L);
    }
}
