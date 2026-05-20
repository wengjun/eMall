package com.emall.common.web;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties("emall.http-client")
@Data
public class OutboundHttpClientProperties {
    private Duration connectTimeout = Duration.ofMillis(300);
    private Duration readTimeout = Duration.ofMillis(800);
    private int maxConnections = 256;
    private int bulkheadMaxConcurrent = 128;
    private int maxAttempts = 2;
    private Duration retryBackoff = Duration.ofMillis(50);
}
