package com.emall.common.security;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.util.AntPathMatcher;

public final class EndpointAuthorizationPolicy {
    private static final Map<String, String> API_DOMAINS = Map.ofEntries(Map.entry("users", "user"),
            Map.entry("products", "product"), Map.entry("inventory", "inventory"), Map.entry("orders", "order"),
            Map.entry("carts", "cart"), Map.entry("payments", "payment"), Map.entry("prices", "pricing"),
            Map.entry("marketing", "marketing"), Map.entry("search", "search"), Map.entry("fulfillment", "fulfillment"),
            Map.entry("reviews", "review"), Map.entry("after-sales", "after-sales"), Map.entry("merchants", "merchant"),
            Map.entry("flash-sales", "flash-sale"), Map.entry("recommendations", "recommendation"),
            Map.entry("cost", "cost"), Map.entry("identity", "identity"), Map.entry("risk", "risk"),
            Map.entry("operations", "operations"), Map.entry("openapi", "openapi"), Map.entry("catalog", "catalog"),
            Map.entry("promotions", "promotion"), Map.entry("experiments", "experiment"),
            Map.entry("advertising", "advertising"), Map.entry("supply-chain", "supply-chain"),
            Map.entry("finance", "finance"), Map.entry("customer-service", "customer-service"),
            Map.entry("forecasting", "forecasting"), Map.entry("event-platform", "event-platform"),
            Map.entry("data-warehouse", "data-warehouse"), Map.entry("intelligence", "intelligence"),
            Map.entry("analytics", "analytics"), Map.entry("traffic", "traffic"),
            Map.entry("reliability", "reliability"), Map.entry("release", "release"),
            Map.entry("platform-ops", "platform-ops"));
    private final AuthSecurityProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public EndpointAuthorizationPolicy(AuthSecurityProperties properties) {
        this.properties = properties;
    }

    public boolean isPublic(String method, String path) {
        return matches(properties.getPublicEndpoints(), method, path);
    }

    public void authorize(AuthenticatedPrincipal principal, String method, String path) {
        if (principal.isPlatformOperator()) {
            return;
        }
        if (matches(properties.getCustomerEndpoints(), method, path) && !principal.isServiceClient()) {
            return;
        }
        if (principal.isServiceClient() && principal.hasScope(requiredServiceScope(path))) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "authenticated identity is not allowed for this endpoint");
    }

    private boolean matches(List<String> endpoints, String method, String path) {
        return endpoints.stream().anyMatch(endpoint -> {
            int delimiter = endpoint.indexOf(':');
            if (delimiter <= 0) {
                return false;
            }
            String configuredMethod = endpoint.substring(0, delimiter);
            return ("*".equals(configuredMethod) || configuredMethod.equalsIgnoreCase(method))
                    && pathMatcher.match(endpoint.substring(delimiter + 1), path);
        });
    }

    private String requiredServiceScope(String path) {
        if (path.startsWith("/internal/")) {
            return "internal:invoke";
        }
        String[] segments = path.split("/");
        if (segments.length <= 2 || !"api".equals(segments[1])) {
            return "internal:invoke";
        }
        return API_DOMAINS.getOrDefault(segments[2], segments[2]) + ":invoke";
    }
}
