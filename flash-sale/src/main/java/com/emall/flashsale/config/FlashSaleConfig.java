package com.emall.flashsale.config;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.flashsale.runtime.FlashSaleSecurityProperties;
import com.emall.flashsale.runtime.FlashSaleTokenSigner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FlashSaleSecurityProperties.class)
public class FlashSaleConfig {
    @Bean
    SnowflakeIdGenerator flashSaleIdGenerator() {
        return new SnowflakeIdGenerator(121L);
    }

    @Bean
    FlashSaleTokenSigner flashSaleTokenSigner(FlashSaleSecurityProperties properties) {
        return new FlashSaleTokenSigner(properties.getTokenSecret());
    }
}
