package com.emall.order.integration;

import com.emall.governance.recovery.AdaptiveRecoveryController;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class InventoryClient {
    private final RestClient inventoryRestClient;
    private final AdaptiveRecoveryController recoveryController;

    public InventoryClient(RestClient inventoryRestClient, AdaptiveRecoveryController recoveryController) {
        this.inventoryRestClient = inventoryRestClient;
        this.recoveryController = recoveryController;
    }

    @Retry(name = "inventoryService")
    @RateLimiter(name = "inventoryService")
    @Bulkhead(name = "inventoryService")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackReserve")
    public InventoryReservation reserve(ReserveInventoryRequest request) {
        if (!recoveryController.allowRequest()) {
            return InventoryReservation.unavailable(
                    request.requestId(), request.skuId(), request.quantity(), "DOWNSTREAM_RECOVERING");
        }
        try {
            InventoryApiResponse response = inventoryRestClient.post()
                    .uri("/api/inventory/reservations")
                    .body(request)
                    .retrieve()
                    .body(InventoryApiResponse.class);
            InventoryReservation result = response == null ? null : response.data();
            recoveryController.recordSuccess();
            return result == null
                    ? InventoryReservation.unavailable(
                            request.requestId(), request.skuId(), request.quantity(), "EMPTY_RESPONSE")
                    : result;
        } catch (RuntimeException ex) {
            recoveryController.recordFailure();
            throw ex;
        }
    }

    public InventoryReservation fallbackReserve(ReserveInventoryRequest request, Throwable error) {
        recoveryController.recordFailure();
        return InventoryReservation.unavailable(
                request.requestId(), request.skuId(), request.quantity(), "FALLBACK_ACCEPTED_FOR_ASYNC_RETRY");
    }

    @Retry(name = "inventoryService")
    @RateLimiter(name = "inventoryService")
    @Bulkhead(name = "inventoryService")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackConfirm")
    public InventoryReservation confirm(String requestId) {
        if (!recoveryController.allowRequest()) {
            return InventoryReservation.unavailable(requestId, 0L, 0, "DOWNSTREAM_RECOVERING");
        }
        try {
            InventoryApiResponse response = inventoryRestClient.post()
                    .uri("/api/inventory/reservations/{requestId}/confirm", requestId)
                    .retrieve()
                    .body(InventoryApiResponse.class);
            InventoryReservation result = response == null ? null : response.data();
            recoveryController.recordSuccess();
            return result == null
                    ? InventoryReservation.unavailable(requestId, 0L, 0, "EMPTY_RESPONSE")
                    : result;
        } catch (RuntimeException ex) {
            recoveryController.recordFailure();
            throw ex;
        }
    }

    public InventoryReservation fallbackConfirm(String requestId, Throwable error) {
        recoveryController.recordFailure();
        return InventoryReservation.unavailable(requestId, 0L, 0, "CONFIRM_FALLBACK_ACCEPTED_FOR_ASYNC_RETRY");
    }

    @Retry(name = "inventoryService")
    @RateLimiter(name = "inventoryService")
    @Bulkhead(name = "inventoryService")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "fallbackRelease")
    public InventoryReservation release(String requestId) {
        if (!recoveryController.allowRequest()) {
            return InventoryReservation.unavailable(requestId, 0L, 0, "DOWNSTREAM_RECOVERING");
        }
        try {
            InventoryApiResponse response = inventoryRestClient.post()
                    .uri("/api/inventory/reservations/{requestId}/release", requestId)
                    .retrieve()
                    .body(InventoryApiResponse.class);
            InventoryReservation result = response == null ? null : response.data();
            recoveryController.recordSuccess();
            return result == null
                    ? InventoryReservation.unavailable(requestId, 0L, 0, "EMPTY_RESPONSE")
                    : result;
        } catch (RuntimeException ex) {
            recoveryController.recordFailure();
            throw ex;
        }
    }

    public InventoryReservation fallbackRelease(String requestId, Throwable error) {
        recoveryController.recordFailure();
        return InventoryReservation.unavailable(requestId, 0L, 0, "RELEASE_FALLBACK_ACCEPTED_FOR_ASYNC_RETRY");
    }

    public record ReserveInventoryRequest(String requestId, long skuId, int quantity) {
    }

    public record InventoryReservation(
            String requestId,
            long skuId,
            int quantity,
            String status,
            String reason,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        public boolean reserved() {
            return "RESERVED".equals(status);
        }

        public boolean confirmed() {
            return "CONFIRMED".equals(status);
        }

        public boolean released() {
            return "RELEASED".equals(status) || "REJECTED".equals(status);
        }

        static InventoryReservation unavailable(String requestId, long skuId, int quantity, String reason) {
            Instant now = Instant.now();
            return new InventoryReservation(requestId, skuId, quantity, "UNAVAILABLE", reason, now, now, now);
        }
    }

    public record InventoryApiResponse(
            boolean success,
            String code,
            String message,
            InventoryReservation data,
            Instant timestamp
    ) {
    }
}
