package com.emall.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.security.AuthenticatedPrincipal;
import com.emall.gateway.filter.GatewayAuthenticationFilter;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class RateLimitConfigTest {
    private final GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
    private final RateLimitConfig config = new RateLimitConfig(properties);
    private final KeyResolver resolver = config.userOrIpKeyResolver();

    @Test
    void shouldUseTrustedProxyChainForAnonymousClientAddress() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/products/30001").remoteAddress(new InetSocketAddress("10.0.0.2", 12345))
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("subject=ip:203.0.113.10|route=products|ip=203.0.113.10");
    }

    @Test
    void shouldIgnoreForgedForwardedHeaderFromUntrustedPeer() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/search").remoteAddress(new InetSocketAddress("198.51.100.9", 12345))
                        .header("X-Forwarded-For", "1.1.1.1").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).contains("subject=ip:198.51.100.9").doesNotContain("1.1.1.1");
    }

    @Test
    void shouldUseVerifiedPrincipalAndIgnoreSpoofableBusinessDimensions() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/1001")
                .remoteAddress(new InetSocketAddress("198.51.100.9", 12345)).header("X-Account-Id", "999")
                .header("X-Device-Id", "rotating-device").header("X-Client-Channel", "rotating-channel")
                .header("X-Forwarded-For", "1.1.1.1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(GatewayAuthenticationFilter.PRINCIPAL_ATTRIBUTE, new AuthenticatedPrincipal(70001L,
                80001L, "customer", "CUSTOMER", Set.of(), Instant.now(), Instant.now().plusSeconds(600)));

        String key = resolver.resolve(exchange).block();

        assertThat(key).isEqualTo("subject=account:70001|route=orders");
    }

    @Test
    void shouldDisableOptionalHighCardinalityDimensionsByDefault() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/30001")
                .remoteAddress(new InetSocketAddress("198.51.100.9", 12345)).header("X-Device-Id", "device-001")
                .header("X-Client-Type", "APP").header("X-Client-Channel", "android-app")
                .header("X-Region-Code", "cn-east").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).doesNotContain("device=", "client=", "channel=", "region=", "sku=");
    }

    @Test
    void shouldIgnoreNonIpForwardedValuesInsteadOfCreatingAttackerControlledKeys() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/search").remoteAddress(new InetSocketAddress("10.0.0.2", 12345))
                        .header("X-Forwarded-For", "rotating-attacker-key.example").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("subject=ip:10.0.0.2|route=search|ip=10.0.0.2");
    }

    @Test
    void shouldIgnoreInvalidNumericForwardedAddress() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/search").remoteAddress(new InetSocketAddress("10.0.0.2", 12345))
                        .header("X-Forwarded-For", "999.999.999.999").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("subject=ip:10.0.0.2|route=search|ip=10.0.0.2");
    }

    @Test
    void shouldUseIndependentGlobalAndCanonicalHotResourceKeys() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/30001?skuId=30001")
                .remoteAddress(new InetSocketAddress("198.51.100.9", 12345)).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        assertThat(config.globalKeyResolver().resolve(exchange).block()).isEqualTo("global");
        assertThat(config.hotResourceKeyResolver().resolve(exchange).block())
                .isEqualTo("route=products|resource=30001");
    }

    @Test
    void shouldCollapseAttackerControlledHotResourceValues() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/not-a-sku?skuId=rotating-value")
                .remoteAddress(new InetSocketAddress("198.51.100.9", 12345)).build();

        String key = config.hotResourceKeyResolver().resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("route=products|resource=none");
    }
}
