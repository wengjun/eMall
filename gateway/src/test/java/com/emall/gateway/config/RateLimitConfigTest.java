package com.emall.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class RateLimitConfigTest {
    private final GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
    private final KeyResolver resolver = new RateLimitConfig(properties).userOrIpKeyResolver();

    @Test
    void shouldUseClientChannelDeviceSkuAndIpInRateLimitKey() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/30001").header("X-Client-Type", "APP")
                .header("X-Client-Channel", "android-app").header("X-Device-Id", "device-001")
                .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1").header("X-Region-Code", "us-east")
                .header("X-Cell-Code", "cell-a").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("user=anonymous|client=APP|channel=android-app|device=device-001|sku=30001|"
                + "campaign=none|region=us-east|cell=cell-a|ip=203.0.113.10");
    }

    @Test
    void shouldDefaultMissingClientHeadersForLegacyClients() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/search").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).contains("client=UNKNOWN").contains("channel=direct").contains("device=unknown-device");
    }

    @Test
    void shouldDisableHighCardinalityDimensionsByConfiguration() {
        properties.setIncludeDevice(false);
        properties.setIncludeIp(false);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/30001")
                .header("X-Device-Id", "device-001").header("X-Forwarded-For", "203.0.113.10").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).doesNotContain("device=").doesNotContain("ip=");
    }
}
