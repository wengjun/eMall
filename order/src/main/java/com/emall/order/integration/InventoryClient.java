package com.emall.order.integration;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.emall.common.rpc.InventoryReservationView;
import com.emall.common.rpc.InventoryRpcService;
import com.emall.common.rpc.ReserveInventoryCommand;
import com.emall.governance.recovery.AdaptiveRecoveryController;
import java.time.Instant;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class InventoryClient {
    private final RestClient inventoryRestClient;
    private final AdaptiveRecoveryController recoveryController;
    private final String rpcProtocol;

    @DubboReference(check = false, retries = 0, timeout = 2000)
    private InventoryRpcService inventoryRpcService;

    @Autowired
    public InventoryClient(RestClient inventoryRestClient, AdaptiveRecoveryController recoveryController,
            @Value("${emall.rpc.protocol:http}") String rpcProtocol) {
        this.inventoryRestClient = inventoryRestClient;
        this.recoveryController = recoveryController;
        this.rpcProtocol = rpcProtocol;
    }

    public InventoryClient(RestClient inventoryRestClient, AdaptiveRecoveryController recoveryController) {
        this(inventoryRestClient, recoveryController, "http");
    }

    @SentinelResource(value = "order.inventory.reserve", blockHandler = "blockReserve", fallback = "fallbackReserve")
    public InventoryReservation reserve(ReserveInventoryRequest request) {
        if (!recoveryController.allowRequest()) {
            return InventoryReservation.unavailable(request.requestId(), request.skuId(), request.quantity(),
                    "DOWNSTREAM_RECOVERING");
        }
        try {
            InventoryReservation result;
            if (dubboEnabled()) {
                result = toLocal(inventoryRpcService.reserve(
                        new ReserveInventoryCommand(request.requestId(), request.skuId(), request.quantity())));
            } else {
                InventoryApiResponse response = inventoryRestClient.post().uri("/api/inventory/reservations")
                        .body(request).retrieve().body(InventoryApiResponse.class);
                result = response == null ? null : response.data();
            }
            recoveryController.recordSuccess();
            return result == null
                    ? InventoryReservation.unavailable(request.requestId(), request.skuId(), request.quantity(),
                            "EMPTY_RESPONSE")
                    : result;
        } catch (RuntimeException ex) {
            recoveryController.recordFailure();
            throw ex;
        }
    }

    public InventoryReservation fallbackReserve(ReserveInventoryRequest request, Throwable error) {
        recoveryController.recordFailure();
        return InventoryReservation.unavailable(request.requestId(), request.skuId(), request.quantity(),
                "FALLBACK_ACCEPTED_FOR_ASYNC_RETRY");
    }

    public InventoryReservation blockReserve(ReserveInventoryRequest request, BlockException error) {
        return InventoryReservation.unavailable(request.requestId(), request.skuId(), request.quantity(),
                "SENTINEL_BLOCKED_FOR_ASYNC_RETRY");
    }

    @SentinelResource(value = "order.inventory.confirm", blockHandler = "blockConfirm", fallback = "fallbackConfirm")
    public InventoryReservation confirm(String requestId) {
        if (!recoveryController.allowRequest()) {
            return InventoryReservation.unavailable(requestId, 0L, 0, "DOWNSTREAM_RECOVERING");
        }
        try {
            InventoryReservation result;
            if (dubboEnabled()) {
                result = toLocal(inventoryRpcService.confirm(requestId));
            } else {
                InventoryApiResponse response =
                        inventoryRestClient.post().uri("/api/inventory/reservations/{requestId}/confirm", requestId)
                                .retrieve().body(InventoryApiResponse.class);
                result = response == null ? null : response.data();
            }
            recoveryController.recordSuccess();
            return result == null ? InventoryReservation.unavailable(requestId, 0L, 0, "EMPTY_RESPONSE") : result;
        } catch (RuntimeException ex) {
            recoveryController.recordFailure();
            throw ex;
        }
    }

    public InventoryReservation fallbackConfirm(String requestId, Throwable error) {
        recoveryController.recordFailure();
        return InventoryReservation.unavailable(requestId, 0L, 0, "CONFIRM_FALLBACK_ACCEPTED_FOR_ASYNC_RETRY");
    }

    public InventoryReservation blockConfirm(String requestId, BlockException error) {
        return InventoryReservation.unavailable(requestId, 0L, 0, "CONFIRM_SENTINEL_BLOCKED_FOR_ASYNC_RETRY");
    }

    @SentinelResource(value = "order.inventory.release", blockHandler = "blockRelease", fallback = "fallbackRelease")
    public InventoryReservation release(String requestId) {
        if (!recoveryController.allowRequest()) {
            return InventoryReservation.unavailable(requestId, 0L, 0, "DOWNSTREAM_RECOVERING");
        }
        try {
            InventoryReservation result;
            if (dubboEnabled()) {
                result = toLocal(inventoryRpcService.release(requestId));
            } else {
                InventoryApiResponse response =
                        inventoryRestClient.post().uri("/api/inventory/reservations/{requestId}/release", requestId)
                                .retrieve().body(InventoryApiResponse.class);
                result = response == null ? null : response.data();
            }
            recoveryController.recordSuccess();
            return result == null ? InventoryReservation.unavailable(requestId, 0L, 0, "EMPTY_RESPONSE") : result;
        } catch (RuntimeException ex) {
            recoveryController.recordFailure();
            throw ex;
        }
    }

    public InventoryReservation fallbackRelease(String requestId, Throwable error) {
        recoveryController.recordFailure();
        return InventoryReservation.unavailable(requestId, 0L, 0, "RELEASE_FALLBACK_ACCEPTED_FOR_ASYNC_RETRY");
    }

    public InventoryReservation blockRelease(String requestId, BlockException error) {
        return InventoryReservation.unavailable(requestId, 0L, 0, "RELEASE_SENTINEL_BLOCKED_FOR_ASYNC_RETRY");
    }

    private boolean dubboEnabled() {
        return "dubbo".equalsIgnoreCase(rpcProtocol) && inventoryRpcService != null;
    }

    private InventoryReservation toLocal(InventoryReservationView view) {
        return view == null
                ? null
                : new InventoryReservation(view.requestId(), view.skuId(), view.quantity(), view.status(),
                        view.reason(), view.expiresAt(), view.createdAt(), view.updatedAt());
    }

    public record ReserveInventoryRequest(String requestId, long skuId, int quantity) {
    }

    public record InventoryReservation(String requestId, long skuId, int quantity, String status, String reason,
            Instant expiresAt, Instant createdAt, Instant updatedAt) {
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

    public record InventoryApiResponse(boolean success, String code, String message, InventoryReservation data,
            Instant timestamp) {
    }
}
