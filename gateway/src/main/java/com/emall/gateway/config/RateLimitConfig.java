package com.emall.gateway.config;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
@RefreshScope
public class RateLimitConfig {
    private final GatewayRateLimitProperties properties;

    public RateLimitConfig(GatewayRateLimitProperties properties) {
        this.properties = properties;
    }

    @Bean
    KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal().map(Principal::getName).defaultIfEmpty("anonymous")
                .map(user -> rateLimitKey(exchange, user));
    }

    private String rateLimitKey(ServerWebExchange exchange, String user) {
        ServerHttpRequest request = exchange.getRequest();
        List<String> parts = new ArrayList<>();
        parts.add("user=" + user);
        addIfEnabled(parts, properties.isIncludeClientType(), "client",
                Optional.ofNullable(request.getHeaders().getFirst("X-Client-Type")).orElse("UNKNOWN"));
        addIfEnabled(parts, properties.isIncludeChannel(), "channel",
                Optional.ofNullable(request.getHeaders().getFirst("X-Client-Channel")).orElse("direct"));
        addIfEnabled(parts, properties.isIncludeDevice(), "device",
                Optional.ofNullable(request.getHeaders().getFirst("X-Device-Id")).orElse("unknown-device"));
        addIfEnabled(parts, properties.isIncludeSku(), "sku",
                Optional.ofNullable(request.getQueryParams().getFirst("skuId"))
                        .orElseGet(() -> skuFromPath(request.getPath().value())));
        addIfEnabled(parts, properties.isIncludeCampaign(), "campaign",
                Optional.ofNullable(request.getQueryParams().getFirst("campaignId")).orElse("none"));
        if (properties.isIncludeRegionCell()) {
            parts.add("region=" + Optional.ofNullable(request.getHeaders().getFirst("X-Region-Code")).orElse("none"));
            parts.add("cell=" + Optional.ofNullable(request.getHeaders().getFirst("X-Cell-Code")).orElse("none"));
        }
        addIfEnabled(parts, properties.isIncludeIp(), "ip", clientIp(exchange));
        return String.join("|", parts);
    }

    private void addIfEnabled(List<String> parts, boolean enabled, String name, String value) {
        if (enabled) {
            parts.add(name + "=" + value);
        }
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
