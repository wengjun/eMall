package com.emall.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiAuthenticationFilterTest {
    private final AuthSecurityProperties properties = properties();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuthTokenCodec tokenCodec = new AuthTokenCodec(objectMapper, properties);
    private final ApiAuthenticationFilter filter =
            new ApiAuthenticationFilter(tokenCodec, new NoopTokenRevocationStore(), properties, objectMapper);

    @AfterEach
    void clearAuthenticationContext() {
        AuthenticationContext.clear();
    }

    @Test
    void protectsDirectServiceRequestsThatBypassTheGateway() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/1001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(invoked).isFalse();
    }

    @Test
    void rejectsCustomerRoleEscalationToOperatorApi() throws Exception {
        MockHttpServletRequest request =
                authenticatedRequest("POST", "/api/identity/service-clients", "CUSTOMER", Set.of());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> invoked.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(invoked).isFalse();
    }

    @Test
    void requiresServiceSpecificScopeForPrivilegedApi() throws Exception {
        MockHttpServletResponse denied = invoke(
                authenticatedRequest("POST", "/api/inventory/reservations", "SERVICE_CLIENT", Set.of("order:invoke")));
        MockHttpServletResponse allowed = invoke(authenticatedRequest("POST", "/api/inventory/reservations",
                "SERVICE_CLIENT", Set.of("inventory:invoke")));

        assertThat(denied.getStatus()).isEqualTo(403);
        assertThat(allowed.getStatus()).isEqualTo(204);
    }

    @Test
    void requiresServiceSpecificScopeEvenForCustomerFacingApi() throws Exception {
        MockHttpServletResponse denied =
                invoke(authenticatedRequest("GET", "/api/orders/1001", "SERVICE_CLIENT", Set.of("payment:invoke")));
        MockHttpServletResponse allowed =
                invoke(authenticatedRequest("GET", "/api/orders/1001", "SERVICE_CLIENT", Set.of("order:invoke")));

        assertThat(denied.getStatus()).isEqualTo(403);
        assertThat(allowed.getStatus()).isEqualTo(204);
    }

    @Test
    void merchantOperatorCannotReadAnotherCustomerResource() {
        AuthenticationContext.set(principal("MERCHANT_OPERATOR", Set.of()));
        AuthorizationGuard guard = new AuthorizationGuard(properties);

        assertThatThrownBy(() -> guard.requireOwnerOrOperator(9999L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
    }

    private MockHttpServletResponse invoke(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, servletResponse) -> {
            ((MockHttpServletResponse) servletResponse).setStatus(204);
            assertThat(AuthenticationContext.current()).isPresent();
        });
        return response;
    }

    private MockHttpServletRequest authenticatedRequest(String method, String path, String identityType,
            Set<String> scopes) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Authorization", "Bearer " + tokenCodec.issue(principal(identityType, scopes)));
        return request;
    }

    private AuthenticatedPrincipal principal(String identityType, Set<String> scopes) {
        Instant now = Instant.now();
        return new AuthenticatedPrincipal(1001L, 2001L, "subject", identityType, scopes, now, now.plusSeconds(600));
    }

    private AuthSecurityProperties properties() {
        AuthSecurityProperties value = new AuthSecurityProperties();
        value.setTokenSecret("direct-service-authentication-test-secret-32-bytes");
        return value;
    }
}
