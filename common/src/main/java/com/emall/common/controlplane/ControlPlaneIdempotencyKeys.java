package com.emall.common.controlplane;

import com.emall.common.idempotency.IdempotencyHeaders;
import com.emall.common.web.TraceHeaders;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.MDC;

public final class ControlPlaneIdempotencyKeys {
    private ControlPlaneIdempotencyKeys() {
    }

    public static String currentOrDeterministic(String module, String action, String deterministicSuffix) {
        String requestKey = MDC.get(IdempotencyHeaders.IDEMPOTENCY_KEY);
        if (requestKey == null || requestKey.isBlank()) {
            requestKey = TraceHeaders.currentRequestId();
        }
        String suffix = requestKey == null || requestKey.isBlank() ? deterministicSuffix : requestKey;
        return Stream.of(module, action, suffix).map(ControlPlaneIdempotencyKeys::normalize)
                .collect(Collectors.joining(":"));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("control-plane idempotency key part must not be blank");
        }
        return value.trim().replace(':', '_');
    }
}
