package com.emall.fulfillment.config;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class FulfillmentConfig {
    @Bean
    SnowflakeIdGenerator fulfillmentIdGenerator() {
        return new SnowflakeIdGenerator(91L);
    }
}
