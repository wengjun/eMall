package com.emall.aftersales.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AfterSalesConfig {
    @Bean
    SnowflakeIdGenerator afterSalesIdGenerator() {
        return new SnowflakeIdGenerator(111L);
    }
}
