package com.emall.gateway.config;

import java.security.Principal;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    @Bean
    KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal().map(Principal::getName).defaultIfEmpty("anonymous")
                .map(user -> rateLimitKey(exchange, user));
    }

    private String rateLimitKey(ServerWebExchange exchange, String user) {
        ServerHttpRequest request = exchange.getRequest();
        String deviceId = Optional.ofNullable(request.getHeaders().getFirst("X-Device-Id")).orElse("unknown-device");
        String skuId = Optional.ofNullable(request.getQueryParams().getFirst("skuId"))
                .orElseGet(() -> skuFromPath(request.getPath().value()));
        return "user=" + user + "|device=" + deviceId + "|sku=" + skuId + "|ip=" + clientIp(exchange);
    }

    private String clientIp(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .map(header -> header.split(",")[0].trim())
                .orElseGet(() -> Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(address -> address.getAddress().getHostAddress()).orElse("unknown-ip"));
    }

    private String skuFromPath(String path) {
        String[] parts = path.split("/");
        for (int index = 0; index < parts.length; index++) {
            if (("products".equals(parts[index]) || "inventory".equals(parts[index])) && index + 1 < parts.length) {
                return parts[index + 1];
            }
        }
        return "none";
    }
}
