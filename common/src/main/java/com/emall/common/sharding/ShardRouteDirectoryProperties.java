package com.emall.common.sharding;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.sharding.route-directory")
public class ShardRouteDirectoryProperties {
    private String endpoint;
    private String internalOperationKey;
    private Duration cacheTtl = Duration.ofHours(6);
    private Map<String, Duration> retention = defaultRetention();

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getInternalOperationKey() {
        return internalOperationKey;
    }

    public void setInternalOperationKey(String internalOperationKey) {
        this.internalOperationKey = internalOperationKey;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public Map<String, Duration> getRetention() {
        return retention;
    }

    public void setRetention(Map<String, Duration> retention) {
        this.retention = retention == null ? defaultRetention() : new LinkedHashMap<>(retention);
    }

    public Duration retentionFor(String namespace) {
        return retention.get(namespace);
    }

    private static Map<String, Duration> defaultRetention() {
        Map<String, Duration> defaults = new LinkedHashMap<>();
        defaults.put("order-request", Duration.ofDays(30));
        defaults.put("payment-request", Duration.ofDays(30));
        defaults.put("inventory-reservation", Duration.ofDays(30));
        defaults.put("flash-sale-request", Duration.ofDays(7));
        return defaults;
    }
}
