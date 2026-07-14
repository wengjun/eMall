package com.emall.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emall.common.security.AuthSecurityProperties;
import com.emall.common.security.AuthTokenCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class GatewayAuthenticationFilterTest {
    private final AuthSecurityProperties properties = new AuthSecurityProperties();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
    private GatewayAuthenticationFilter filter;
    private AuthTokenCodec tokenCodec;

    @BeforeEach
    void setUp() {
        properties.setTokenSecret("gateway-authentication-test-secret-32-bytes");
        properties.setFailClosedOnRevocationStoreError(true);
        tokenCodec = new AuthTokenCodec(objectMapper, properties);
        filter = new GatewayAuthenticationFilter(tokenCodec, properties, redisTemplate, objectMapper);
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
    }

    @Test
    void rejectsAnonymousRequestsToProtectedApis() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1001").build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void stripsForgedIdentityHeadersEvenOnPublicEndpoints() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products/3001")
                .header("X-Account-Id", "999").header("X-Authenticated-Account-Id", "999").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capture(forwarded)).block();

        assertThat(forwarded.get().getRequest().getHeaders().containsKey("X-Account-Id")).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders().containsKey("X-Authenticated-Account-Id")).isFalse();
    }

    @Test
    void forwardsOnlyIdentityDerivedFromVerifiedToken() {
        String token = tokenCodec.issue(70001L, 80001L, "customer", "CUSTOMER", Set.of("order:read"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1001")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).header("X-Account-Id", "999").build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, capture(forwarded)).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Authenticated-Account-Id")).isEqualTo("70001");
        assertThat(forwarded.get().getRequest().getHeaders().containsKey("X-Account-Id")).isFalse();
        Object principal = forwarded.get().getAttribute(GatewayAuthenticationFilter.PRINCIPAL_ATTRIBUTE);
        assertThat(principal).isNotNull();
    }

    @Test
    void rejectsCustomerTokenOnOperatorEndpoint() {
        String token = tokenCodec.issue(70001L, 80001L, "customer", "CUSTOMER", Set.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/api/identity/service-clients").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void failsClosedWhenRevocationStoreIsUnavailable() {
        String token = tokenCodec.issue(70001L, 80001L, "customer", "CUSTOMER", Set.of());
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.error(new IllegalStateException("redis offline")));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1001")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void doesNotMisclassifyDownstreamFailureAsRevocationFailure() {
        String token = tokenCodec.issue(70001L, 80001L, "customer", "CUSTOMER", Set.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders/1001")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        assertThatThrownBy(
                () -> filter.filter(exchange, ignored -> Mono.error(new IllegalStateException("downstream"))).block())
                .isInstanceOf(IllegalStateException.class).hasMessage("downstream");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private GatewayFilterChain capture(AtomicReference<ServerWebExchange> forwarded) {
        return exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }
}
