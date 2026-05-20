package com.emall.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboundHttpClientFactoryTest {
    @Test
    void shouldExposeCombinedTimeoutBudgetAndCreateRestClient() {
        OutboundHttpClientProperties properties = new OutboundHttpClientProperties();
        properties.setConnectTimeout(Duration.ofMillis(250));
        properties.setReadTimeout(Duration.ofMillis(750));
        properties.setMaxConnections(2);
        OutboundHttpClientFactory factory = new OutboundHttpClientFactory(properties);

        assertThat(factory.timeoutBudget()).isEqualTo(Duration.ofMillis(1000));
        assertThat(factory.restClient("product", "http://product-app:8082")).isNotNull();
    }
}
