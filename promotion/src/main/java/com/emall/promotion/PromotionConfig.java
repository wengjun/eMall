package com.emall.promotion;

import com.emall.common.id.SnowflakeIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PromotionConfig {
    @Bean
    SnowflakeIdGenerator promotionIdGenerator() {
        return new SnowflakeIdGenerator(162L);
    }
}
