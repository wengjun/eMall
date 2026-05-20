package com.emall.common.web;

import com.emall.common.idempotency.IdempotencyHeaders;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@ConfigurationProperties(prefix = "emall.idempotency.http")
@Data
public class IdempotencyHttpProperties {
    private boolean requireWriteKey;
    private String headerName = IdempotencyHeaders.IDEMPOTENCY_KEY;
    private Set<String> excludedPathPrefixes = new LinkedHashSet<>(Set.of("/actuator", "/internal"));
}
