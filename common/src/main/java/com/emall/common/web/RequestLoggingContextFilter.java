package com.emall.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestLoggingContextFilter extends OncePerRequestFilter {
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("com.emall.access");
    private static final Pattern ID_SEGMENT = Pattern.compile("/[0-9A-Za-z][0-9A-Za-z_-]{5,}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Map<String, String> previous = captureMdc();
        long startNanos = System.nanoTime();
        putIfPresent(TraceHeaders.REQUEST_ID_MDC_KEY, requestId(request));
        putIfPresent(TraceHeaders.USER_ID_MDC_KEY, firstPresent(
                request.getHeader(TraceHeaders.USER_ID_HEADER),
                request.getParameter("userId")));
        putIfPresent(TraceHeaders.ORDER_ID_MDC_KEY, orderId(request));
        MDC.put(TraceHeaders.ROUTE_MDC_KEY, route(request));
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = Math.max(1L, (System.nanoTime() - startNanos) / 1_000_000L);
            MDC.put(TraceHeaders.STATUS_MDC_KEY, Integer.toString(response.getStatus()));
            MDC.put(TraceHeaders.LATENCY_MS_MDC_KEY, Long.toString(latencyMs));
            ACCESS_LOG.info("request completed");
            restoreMdc(previous);
        }
    }

    private Map<String, String> captureMdc() {
        Map<String, String> previous = new LinkedHashMap<>();
        previous.put(TraceHeaders.REQUEST_ID_MDC_KEY, MDC.get(TraceHeaders.REQUEST_ID_MDC_KEY));
        previous.put(TraceHeaders.USER_ID_MDC_KEY, MDC.get(TraceHeaders.USER_ID_MDC_KEY));
        previous.put(TraceHeaders.ORDER_ID_MDC_KEY, MDC.get(TraceHeaders.ORDER_ID_MDC_KEY));
        previous.put(TraceHeaders.ROUTE_MDC_KEY, MDC.get(TraceHeaders.ROUTE_MDC_KEY));
        previous.put(TraceHeaders.STATUS_MDC_KEY, MDC.get(TraceHeaders.STATUS_MDC_KEY));
        previous.put(TraceHeaders.LATENCY_MS_MDC_KEY, MDC.get(TraceHeaders.LATENCY_MS_MDC_KEY));
        return previous;
    }

    private void restoreMdc(Map<String, String> previous) {
        previous.forEach((key, value) -> {
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });
    }

    private String requestId(HttpServletRequest request) {
        return TraceHeaders.normalizeOrCreate(firstPresent(
                request.getHeader(TraceHeaders.REQUEST_ID_HEADER),
                request.getParameter("requestId")));
    }

    private String orderId(HttpServletRequest request) {
        return firstPresent(request.getParameter("orderId"), idAfterSegment(request.getRequestURI(), "orders"));
    }

    private String route(HttpServletRequest request) {
        return request.getMethod() + " " + ID_SEGMENT.matcher(request.getRequestURI()).replaceAll("/{id}");
    }

    private String idAfterSegment(String path, String segment) {
        String[] parts = Optional.ofNullable(path).orElse("").split("/");
        for (int index = 0; index < parts.length - 1; index++) {
            if (segment.equals(parts[index]) && !parts[index + 1].isBlank()) {
                return parts[index + 1];
            }
        }
        return null;
    }

    private String firstPresent(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
