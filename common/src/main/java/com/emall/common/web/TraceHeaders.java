package com.emall.common.web;

import java.util.UUID;
import org.slf4j.MDC;

public final class TraceHeaders {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String ORDER_ID_MDC_KEY = "orderId";
    public static final String ROUTE_MDC_KEY = "route";
    public static final String STATUS_MDC_KEY = "status";
    public static final String LATENCY_MS_MDC_KEY = "latencyMs";

    private TraceHeaders() {
    }

    public static String currentTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }

    public static String currentRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }

    public static String normalizeOrCreate(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return newTraceId();
        }
        return traceId.trim();
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
