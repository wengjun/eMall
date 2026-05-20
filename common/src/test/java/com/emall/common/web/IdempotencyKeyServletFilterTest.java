package com.emall.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.idempotency.IdempotencyHeaders;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IdempotencyKeyServletFilterTest {
    @Test
    void shouldAllowWritesWhenRequirementDisabled() throws Exception {
        IdempotencyHttpProperties properties = new IdempotencyHttpProperties();
        IdempotencyKeyServletFilter filter = new IdempotencyKeyServletFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CountingFilterChain chain = new CountingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.count).isEqualTo(1);
    }

    @Test
    void shouldRejectWriteWithoutIdempotencyKeyWhenRequirementEnabled() throws Exception {
        IdempotencyHttpProperties properties = new IdempotencyHttpProperties();
        properties.setRequireWriteKey(true);
        IdempotencyKeyServletFilter filter = new IdempotencyKeyServletFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new CountingFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void shouldAllowWriteWithIdempotencyKey() throws Exception {
        IdempotencyHttpProperties properties = new IdempotencyHttpProperties();
        properties.setRequireWriteKey(true);
        IdempotencyKeyServletFilter filter = new IdempotencyKeyServletFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader(IdempotencyHeaders.IDEMPOTENCY_KEY, "request-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CountingFilterChain chain = new CountingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.count).isEqualTo(1);
    }

    private static final class CountingFilterChain implements FilterChain {
        private int count;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
            count++;
        }
    }
}
