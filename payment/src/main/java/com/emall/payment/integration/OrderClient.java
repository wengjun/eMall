package com.emall.payment.integration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrderClient {
    private final RestClient orderRestClient;

    public OrderClient(RestClient orderRestClient) {
        this.orderRestClient = orderRestClient;
    }

    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackPay")
    public boolean payOrder(long orderId) {
        orderRestClient.post().uri("/api/orders/{orderId}/pay", orderId).retrieve().toBodilessEntity();
        return true;
    }

    public boolean fallbackPay(long orderId, Throwable error) {
        return false;
    }
}
