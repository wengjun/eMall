package com.emall.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateTraceIdWhenRequestDoesNotProvideOne() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products").build());
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwardedExchange.set(current);
            return Mono.empty();
        }).block();

        String responseTraceId = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.TRACE_ID_HEADER);
        String forwardedTraceId =
                forwardedExchange.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.TRACE_ID_HEADER);

        assertThat(responseTraceId).isNotBlank();
        assertThat(forwardedTraceId).isEqualTo(responseTraceId);
    }

    @Test
    void shouldPreserveExistingTraceId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/products")
                .header(CorrelationIdFilter.TRACE_ID_HEADER, "trace-001").build());
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwardedExchange.set(current);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.TRACE_ID_HEADER))
                .isEqualTo("trace-001");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.TRACE_ID_HEADER))
                .isEqualTo("trace-001");
    }
}
