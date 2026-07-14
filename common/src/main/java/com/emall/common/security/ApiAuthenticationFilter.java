package com.emall.common.security;

import com.emall.common.api.ApiResponse;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

final class ApiAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private final AuthTokenCodec tokenCodec;
    private final TokenRevocationStore revocationStore;
    private final AuthSecurityProperties properties;
    private final ObjectMapper objectMapper;
    private final EndpointAuthorizationPolicy authorizationPolicy;

    ApiAuthenticationFilter(AuthTokenCodec tokenCodec, TokenRevocationStore revocationStore,
            AuthSecurityProperties properties, ObjectMapper objectMapper) {
        this.tokenCodec = tokenCodec;
        this.revocationStore = revocationStore;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.authorizationPolicy = new EndpointAuthorizationPolicy(properties);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
                if (isPublic(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "bearer access token is required");
            }
            AuthenticatedPrincipal principal = tokenCodec.verify(authorization.substring(BEARER_PREFIX.length()));
            verifyNotRevoked(principal.sessionId());
            authorizationPolicy.authorize(principal, request.getMethod(), request.getRequestURI());
            AuthenticationContext.set(principal);
            request.setAttribute(AuthenticatedPrincipal.class.getName(), principal);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            writeFailure(response, ex);
        } finally {
            AuthenticationContext.clear();
        }
    }

    private boolean isPublic(HttpServletRequest request) {
        return authorizationPolicy.isPublic(request.getMethod(), request.getRequestURI());
    }

    private void verifyNotRevoked(long sessionId) {
        try {
            if (revocationStore.isRevoked(sessionId)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "access token has been revoked");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (properties.isFailClosedOnRevocationStoreError()) {
                throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "token revocation store is unavailable");
            }
            LOGGER.warn("Token revocation lookup failed; local mode is configured to fail open", ex);
        }
    }

    private void writeFailure(HttpServletResponse response, BusinessException exception) throws IOException {
        int status = exception.errorCode() == ErrorCode.UNAUTHORIZED
                ? HttpServletResponse.SC_UNAUTHORIZED
                : exception.errorCode() == ErrorCode.DOWNSTREAM_UNAVAILABLE
                        ? HttpServletResponse.SC_SERVICE_UNAVAILABLE
                        : HttpServletResponse.SC_FORBIDDEN;
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.fail(exception.errorCode().name(), exception.getMessage()));
    }
}
