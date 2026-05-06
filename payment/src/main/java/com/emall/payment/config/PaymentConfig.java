package com.emall.payment.config;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.web.TraceIdClientHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PaymentConfig {
    @Bean
    SnowflakeIdGenerator paymentIdGenerator() {
        return new SnowflakeIdGenerator(61L);
    }

    @Bean
    RestClient orderRestClient(@Value("${emall.downstream.order-url}") String orderUrl) {
        return RestClient.builder()
                .baseUrl(orderUrl)
                .requestInterceptor(new TraceIdClientHttpRequestInterceptor())
                .build();
    }
}
