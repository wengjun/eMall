package com.emall.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorrelationIdServletFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String previousTraceId = MDC.get(TraceHeaders.TRACE_ID_MDC_KEY);
        String traceId = TraceHeaders.normalizeOrCreate(request.getHeader(TraceHeaders.TRACE_ID_HEADER));
        MDC.put(TraceHeaders.TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TraceHeaders.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (previousTraceId == null) {
                MDC.remove(TraceHeaders.TRACE_ID_MDC_KEY);
            } else {
                MDC.put(TraceHeaders.TRACE_ID_MDC_KEY, previousTraceId);
            }
        }
    }
}
