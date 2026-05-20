package com.emall.common.web;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;

public class OutboundHttpClientInterceptor implements ClientHttpRequestInterceptor {
    private static final ConcurrentMap<String, Semaphore> BULKHEADS = new ConcurrentHashMap<>();

    private final String clientName;
    private final OutboundHttpClientProperties properties;
    private final Semaphore bulkhead;

    public OutboundHttpClientInterceptor(String clientName, OutboundHttpClientProperties properties) {
        this.clientName = clientName;
        this.properties = properties;
        this.bulkhead =
                BULKHEADS.computeIfAbsent(clientName, ignored -> new Semaphore(properties.getBulkheadMaxConcurrent()));
    }

    @Override
    public ClientHttpResponse intercept(org.springframework.http.HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        if (!bulkhead.tryAcquire()) {
            throw exception(OutboundClientErrorCategory.BULKHEAD_REJECTED, "bulkhead rejected", null);
        }
        try {
            return executeWithRetry(request, body, execution);
        } finally {
            bulkhead.release();
        }
    }

    private ClientHttpResponse executeWithRetry(org.springframework.http.HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        int attempts = Math.max(1, properties.getMaxAttempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                if (shouldRetry(response.getStatusCode()) && attempt < attempts) {
                    response.close();
                    sleepBackoff(attempt);
                    continue;
                }
                return response;
            } catch (SocketTimeoutException | ResourceAccessException ex) {
                if (attempt == attempts) {
                    throw exception(OutboundClientErrorCategory.TIMEOUT, "downstream timeout", ex);
                }
                sleepBackoff(attempt);
            } catch (IOException ex) {
                if (attempt == attempts) {
                    throw exception(OutboundClientErrorCategory.TRANSPORT_ERROR, "downstream transport error", ex);
                }
                sleepBackoff(attempt);
            }
        }
        throw exception(OutboundClientErrorCategory.TRANSPORT_ERROR, "downstream retry exhausted", null);
    }

    private boolean shouldRetry(HttpStatusCode statusCode) {
        return statusCode.is5xxServerError() || statusCode.value() == 429;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(Math.min(properties.getRetryBackoff().toMillis() * attempt, 500L));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw exception(OutboundClientErrorCategory.TRANSPORT_ERROR, "retry backoff interrupted", ex);
        }
    }

    private OutboundClientException exception(OutboundClientErrorCategory category, String message, Throwable cause) {
        return new OutboundClientException(clientName, category, message, cause);
    }
}
