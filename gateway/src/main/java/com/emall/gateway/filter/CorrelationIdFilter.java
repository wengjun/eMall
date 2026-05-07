package com.emall.gateway.filter;

import java.util.List;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = first(exchange.getRequest().getHeaders().get(TRACE_ID_HEADER));
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        ServerHttpRequest request = exchange.getRequest().mutate().header(TRACE_ID_HEADER, traceId).build();
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
