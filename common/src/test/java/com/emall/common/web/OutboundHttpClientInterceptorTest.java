package com.emall.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class OutboundHttpClientInterceptorTest {
    @Test
    void shouldRetryRetriableServerResponse() throws Exception {
        OutboundHttpClientProperties properties = new OutboundHttpClientProperties();
        properties.setMaxAttempts(2);
        properties.setRetryBackoff(Duration.ZERO);
        OutboundHttpClientInterceptor interceptor = new OutboundHttpClientInterceptor("pricing-retry-test", properties);
        ClientHttpResponse first = response(HttpStatus.INTERNAL_SERVER_ERROR);
        ClientHttpResponse second = response(HttpStatus.OK);
        AtomicInteger calls = new AtomicInteger();
        ClientHttpRequestExecution execution = (request, body) -> calls.incrementAndGet() == 1 ? first : second;

        ClientHttpResponse result = interceptor.intercept(mock(HttpRequest.class), new byte[0], execution);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(calls).hasValue(2);
    }

    @Test
    void shouldRejectWhenBulkheadIsFull() throws Exception {
        OutboundHttpClientProperties properties = new OutboundHttpClientProperties();
        properties.setBulkheadMaxConcurrent(0);
        OutboundHttpClientInterceptor interceptor =
                new OutboundHttpClientInterceptor("inventory-bulkhead-test", properties);

        assertThatThrownBy(() -> interceptor.intercept(mock(HttpRequest.class), new byte[0],
                (request, body) -> response(HttpStatus.OK))).isInstanceOf(OutboundClientException.class)
                .satisfies(error -> assertThat(((OutboundClientException) error).category())
                        .isEqualTo(OutboundClientErrorCategory.BULKHEAD_REJECTED));
    }

    private ClientHttpResponse response(HttpStatus status) throws IOException {
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getStatusCode()).thenReturn(status);
        return response;
    }
}
