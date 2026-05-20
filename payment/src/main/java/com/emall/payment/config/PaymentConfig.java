package com.emall.payment.config;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.web.OutboundHttpClientFactory;
import com.emall.payment.security.PaymentCallbackVerifier;
import com.emall.payment.security.PaymentSecurityProperties;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PaymentSecurityProperties.class)
public class PaymentConfig {
    @Bean
    SnowflakeIdGenerator paymentIdGenerator() {
        return new SnowflakeIdGenerator(61L);
    }

    @Bean
    RestClient orderRestClient(OutboundHttpClientFactory httpClientFactory,
            @Value("${emall.downstream.order-url}") String orderUrl) {
        return httpClientFactory.restClient("order", orderUrl);
    }

    @Bean
    PaymentCallbackVerifier paymentCallbackVerifier(PaymentSecurityProperties properties) {
        return new PaymentCallbackVerifier(properties, Clock.systemUTC());
    }
}
