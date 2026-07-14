package com.emall.gateway.config;

import com.emall.common.security.AuthenticatedPrincipal;
import com.emall.gateway.filter.GatewayAuthenticationFilter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
@RefreshScope
public class RateLimitConfig {
    private static final Pattern UNSAFE_COMPONENT = Pattern.compile("[^a-zA-Z0-9._:-]");
    private final GatewayRateLimitProperties properties;

    public RateLimitConfig(GatewayRateLimitProperties properties) {
        this.properties = properties;
    }

    @Bean
    KeyResolver userOrIpKeyResolver() {
        return exchange -> Mono.just(rateLimitKey(exchange));
    }

    @Bean
    KeyResolver globalKeyResolver() {
        return exchange -> Mono.just("global");
    }

    @Bean
    KeyResolver hotResourceKeyResolver() {
        return exchange -> Mono.just(hotResourceKey(exchange.getRequest()));
    }

    @Bean
    RedisRateLimiter globalRedisRateLimiter() {
        return new RedisRateLimiter(properties.getGlobalReplenishRate(), properties.getGlobalBurstCapacity());
    }

    @Bean
    RedisRateLimiter subjectRedisRateLimiter() {
        return new RedisRateLimiter(properties.getSubjectReplenishRate(), properties.getSubjectBurstCapacity());
    }

    @Bean
    RedisRateLimiter hotResourceRedisRateLimiter() {
        return new RedisRateLimiter(properties.getHotResourceReplenishRate(), properties.getHotResourceBurstCapacity());
    }

    private String rateLimitKey(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        AuthenticatedPrincipal principal = exchange.getAttribute(GatewayAuthenticationFilter.PRINCIPAL_ATTRIBUTE);
        List<String> parts = new ArrayList<>();
        if (principal == null) {
            parts.add("subject=ip:" + safe(clientIp(exchange)));
        } else {
            parts.add("subject=account:" + principal.accountId());
        }
        if (properties.isIncludeRoute()) {
            parts.add("route=" + safe(routeGroup(request.getPath().value())));
        }
        addIfEnabled(parts, principal == null && properties.isIncludeIp(), "ip", clientIp(exchange));
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
            parts.add("region="
                    + safe(Optional.ofNullable(request.getHeaders().getFirst("X-Region-Code")).orElse("none")));
            parts.add("cell=" + safe(Optional.ofNullable(request.getHeaders().getFirst("X-Cell-Code")).orElse("none")));
        }
        return String.join("|", parts);
    }

    private void addIfEnabled(List<String> parts, boolean enabled, String name, String value) {
        if (enabled) {
            parts.add(name + "=" + safe(value));
        }
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String remoteIp = remoteAddress == null || remoteAddress.getAddress() == null
                ? "unknown-ip"
                : remoteAddress.getAddress().getHostAddress();
        if (!isTrustedProxy(remoteIp)) {
            return remoteIp;
        }
        List<String> chain = new ArrayList<>();
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null) {
            for (String address : forwardedFor.split(",")) {
                if (!address.isBlank()) {
                    chain.add(address.trim());
                }
            }
        }
        chain.add(remoteIp);
        Collections.reverse(chain);
        for (String address : chain) {
            if (!isIpLiteral(address)) {
                continue;
            }
            if (!isTrustedProxy(address)) {
                return address;
            }
        }
        return remoteIp;
    }

    private boolean isTrustedProxy(String address) {
        return properties.getTrustedProxyCidrs().stream().anyMatch(cidr -> isInCidr(address, cidr));
    }

    private boolean isInCidr(String address, String cidr) {
        try {
            String[] parts = cidr.split("/", -1);
            if (parts.length != 2) {
                return false;
            }
            byte[] candidate = parseIpLiteral(address);
            byte[] network = parseIpLiteral(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);
            if (candidate == null || network == null || candidate.length != network.length || prefixLength < 0
                    || prefixLength > candidate.length * 8) {
                return false;
            }
            int completeBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int index = 0; index < completeBytes; index++) {
                if (candidate[index] != network[index]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xff << (8 - remainingBits);
            return (candidate[completeBytes] & mask) == (network[completeBytes] & mask);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isIpLiteral(String value) {
        return parseIpLiteral(value) != null;
    }

    private byte[] parseIpLiteral(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            String[] octets = value.split("\\.");
            byte[] address = new byte[4];
            for (int index = 0; index < octets.length; index++) {
                int octet = Integer.parseInt(octets[index]);
                if (octet > 255) {
                    return null;
                }
                address[index] = (byte) octet;
            }
            return address;
        }
        if (!value.contains(":") || !value.matches("[0-9a-fA-F:.]+")) {
            return null;
        }
        try {
            return InetAddress.getByName(value).getAddress();
        } catch (java.net.UnknownHostException ex) {
            return null;
        }
    }

    private String routeGroup(String path) {
        String[] parts = path.split("/");
        return parts.length > 2 && "api".equals(parts[1]) ? parts[2] : "unknown";
    }

    private String safe(String value) {
        String sanitized = UNSAFE_COMPONENT.matcher(value == null ? "unknown" : value).replaceAll("_");
        return sanitized.substring(0, Math.min(sanitized.length(), properties.getMaximumComponentLength()));
    }

    private String skuFromPath(String path) {
        String[] parts = path.split("/");
        for (int index = 0; index < parts.length; index++) {
            if (("products".equals(parts[index]) || "inventory".equals(parts[index])
                    || "flash-sales".equals(parts[index])) && index + 1 < parts.length) {
                return parts[index + 1];
            }
        }
        return "none";
    }

    private String hotResourceKey(ServerHttpRequest request) {
        String route = routeGroup(request.getPath().value());
        String candidate = Optional.ofNullable(request.getQueryParams().getFirst("skuId"))
                .orElseGet(() -> Optional.ofNullable(request.getQueryParams().getFirst("campaignId"))
                        .orElseGet(() -> skuFromPath(request.getPath().value())));
        String resource = candidate != null && candidate.matches("[0-9]{1,20}") ? candidate : "none";
        return "route=" + route + "|resource=" + resource;
    }
}
