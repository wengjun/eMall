package com.emall.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class IdempotencyKeyServletFilter extends OncePerRequestFilter {
    private final IdempotencyHttpProperties properties;

    public IdempotencyKeyServletFilter(IdempotencyHttpProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!requiresKey(request) || hasKey(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.sendError(HttpStatus.BAD_REQUEST.value(),
                properties.getHeaderName() + " header or requestId parameter is required for write requests");
    }

    private boolean requiresKey(HttpServletRequest request) {
        if (!properties.isRequireWriteKey()) {
            return false;
        }
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if (!("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method))) {
            return false;
        }
        String path = request.getRequestURI();
        return properties.getExcludedPathPrefixes().stream().noneMatch(path::startsWith);
    }

    private boolean hasKey(HttpServletRequest request) {
        return present(request.getHeader(properties.getHeaderName()))
                || present(request.getHeader(TraceHeaders.REQUEST_ID_HEADER))
                || present(request.getParameter("requestId"));
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
