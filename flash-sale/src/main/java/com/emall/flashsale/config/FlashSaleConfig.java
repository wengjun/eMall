package com.emall.flashsale.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlashSaleConfig {
    @Bean
    SnowflakeIdGenerator flashSaleIdGenerator() {
        return new SnowflakeIdGenerator(121L);
    }
}
