package com.emall.gateway.filter;

import com.emall.common.api.ApiResponse;
import com.emall.common.exception.BusinessException;
import com.emall.common.security.AuthSecurityProperties;
import com.emall.common.security.AuthTokenCodec;
import com.emall.common.security.AuthenticatedPrincipal;
import com.emall.common.security.EndpointAuthorizationPolicy;
import com.emall.common.security.RedisTokenRevocationStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {
    public static final String PRINCIPAL_ATTRIBUTE = GatewayAuthenticationFilter.class.getName() + ".principal";
    private static final String BEARER_PREFIX = "Bearer ";
    private final AuthTokenCodec tokenCodec;
    private final AuthSecurityProperties properties;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final EndpointAuthorizationPolicy authorizationPolicy;

    public GatewayAuthenticationFilter(AuthTokenCodec tokenCodec, AuthSecurityProperties properties,
            ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.tokenCodec = tokenCodec;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.authorizationPolicy = new EndpointAuthorizationPolicy(properties);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = stripUntrustedIdentityHeaders(exchange.getRequest());
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();
        if (!properties.isEnabled()) {
            return chain.filter(sanitizedExchange);
        }
        String authorization = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return isPublic(sanitizedRequest)
                    ? chain.filter(sanitizedExchange)
                    : unauthorized(sanitizedExchange, "bearer access token is required");
        }
        AuthenticatedPrincipal principal;
        try {
            principal = tokenCodec.verify(authorization.substring(BEARER_PREFIX.length()));
        } catch (BusinessException ex) {
            return unauthorized(sanitizedExchange, ex.getMessage());
        }
        Mono<RevocationLookup> revocationLookup =
                redisTemplate.hasKey(RedisTokenRevocationStore.KEY_PREFIX + principal.sessionId())
                        .map(isRevoked -> new RevocationLookup(true, Boolean.TRUE.equals(isRevoked)))
                        .onErrorReturn(new RevocationLookup(false, false));
        return revocationLookup.flatMap(lookup -> {
            if (!lookup.available() && properties.isFailClosedOnRevocationStoreError()) {
                return unavailable(sanitizedExchange);
            }
            if (lookup.revoked()) {
                return unauthorized(sanitizedExchange, "access token has been revoked");
            }
            try {
                authorizationPolicy.authorize(principal, sanitizedRequest.getMethod().name(),
                        sanitizedRequest.getPath().value());
            } catch (BusinessException ex) {
                return forbidden(sanitizedExchange, ex.getMessage());
            }
            ServerHttpRequest authenticatedRequest =
                    sanitizedRequest.mutate().header("X-Authenticated-Account-Id", Long.toString(principal.accountId()))
                            .header("X-Authenticated-Identity-Type", principal.identityType())
                            .header("X-Authenticated-Session-Id", Long.toString(principal.sessionId())).build();
            ServerWebExchange authenticatedExchange = sanitizedExchange.mutate().request(authenticatedRequest).build();
            authenticatedExchange.getAttributes().put(PRINCIPAL_ATTRIBUTE, principal);
            return chain.filter(authenticatedExchange);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private ServerHttpRequest stripUntrustedIdentityHeaders(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            headers.remove("X-Account-Id");
            headers.remove("X-Authenticated-Account-Id");
            headers.remove("X-Authenticated-Identity-Type");
            headers.remove("X-Authenticated-Session-Id");
        }).build();
    }

    private boolean isPublic(ServerHttpRequest request) {
        return authorizationPolicy.isPublic(request.getMethod().name(), request.getPath().value());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeFailure(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return writeFailure(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    private Mono<Void> unavailable(ServerWebExchange exchange) {
        return writeFailure(exchange, HttpStatus.SERVICE_UNAVAILABLE, "DOWNSTREAM_UNAVAILABLE",
                "token revocation store is unavailable");
    }

    private Mono<Void> writeFailure(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(ApiResponse.fail(code, message));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
        } catch (JsonProcessingException ex) {
            return exchange.getResponse().setComplete();
        }
    }

    private record RevocationLookup(boolean available, boolean revoked) {
    }
}
