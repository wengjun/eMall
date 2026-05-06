package com.emall.advertising;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AdvertisingConfig {
    @Bean
    SnowflakeIdGenerator advertisingIdGenerator() {
        return new SnowflakeIdGenerator(164L);
    }
}
