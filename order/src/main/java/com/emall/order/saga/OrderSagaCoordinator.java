package com.emall.order.saga;

import com.emall.order.integration.InventoryClient;
import com.emall.order.integration.MarketingClient;
import com.emall.order.repository.OrderRepository;
import com.emall.common.sharding.ShardRoutingOperations;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OrderSagaCoordinator {
    private static final int MAXIMUM_COMPENSATION_ATTEMPTS = 12;
    private final OrderSagaStateService stateService;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final MarketingClient marketingClient;
    private final ShardRoutingOperations shardRoutingOperations;

    public OrderSagaCoordinator(OrderSagaStateService stateService, OrderRepository orderRepository,
            InventoryClient inventoryClient, MarketingClient marketingClient,
            ShardRoutingOperations shardRoutingOperations) {
        this.stateService = stateService;
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.marketingClient = marketingClient;
        this.shardRoutingOperations = shardRoutingOperations;
    }

    public static OrderSagaCoordinator local(OrderRepository orderRepository, InventoryClient inventoryClient,
            MarketingClient marketingClient) {
        return new OrderSagaCoordinator(new OrderSagaStateService(new InMemoryOrderSagaRepository()), orderRepository,
                inventoryClient, marketingClient, ShardRoutingOperations.noop());
    }

    public OrderCreateSaga start(long sagaId, String requestId, long orderId, long userId, long skuId) {
        return stateService.find(requestId).map(existing -> {
            if (existing.userId() != userId || existing.skuId() != skuId) {
                throw new IllegalStateException("order saga request ID was reused with different arguments");
            }
            if (existing.status() == OrderSagaStatus.COMPENSATED) {
                return stateService.save(existing.restart(Instant.now()));
            }
            if (existing.status() != OrderSagaStatus.RUNNING) {
                throw new IllegalStateException("order saga is not restartable from " + existing.status());
            }
            return existing;
        }).orElseGet(() -> stateService
                .save(OrderCreateSaga.start(sagaId, requestId, orderId, userId, skuId, Instant.now())));
    }

    public OrderCreateSaga advance(OrderCreateSaga saga, OrderSagaStage stage, String couponId,
            String inventoryReservationId) {
        OrderCreateSaga current = stateService.require(saga.requestId());
        if (current.status() != OrderSagaStatus.RUNNING) {
            return current;
        }
        return stateService.save(current.advance(stage, couponId, inventoryReservationId));
    }

    public void completeAfterCommit(OrderCreateSaga saga) {
        shardRoutingOperations.execute("order_create_saga", saga.userId(), () -> {
            OrderCreateSaga current = stateService.require(saga.requestId());
            if (current.status() == OrderSagaStatus.RUNNING) {
                stateService.save(current.status(OrderSagaStatus.COMPLETED, null, null));
            }
            return null;
        });
    }

    public void compensateAfterRollback(OrderCreateSaga saga, String error) {
        shardRoutingOperations.execute("order_create_saga", saga.userId(), () -> {
            compensateInShard(saga.requestId(), error);
            return null;
        });
    }

    public void recover(OrderCreateSaga saga) {
        shardRoutingOperations.execute("order_create_saga", saga.userId(), () -> {
            OrderCreateSaga current = stateService.require(saga.requestId());
            if (current.stage() == OrderSagaStage.ORDER_PERSISTED
                    && orderRepository.findById(current.orderId()).isPresent()) {
                stateService.save(current.status(OrderSagaStatus.COMPLETED, null, null));
            } else {
                compensateInShard(current.requestId(), "stale order creation saga");
            }
            return null;
        });
    }

    private void compensateInShard(String requestId, String error) {
        OrderCreateSaga current = stateService.require(requestId);
        if (current.status() == OrderSagaStatus.COMPLETED || current.status() == OrderSagaStatus.COMPENSATED) {
            return;
        }
        if (current.attempts() >= MAXIMUM_COMPENSATION_ATTEMPTS) {
            stateService.save(current.status(OrderSagaStatus.MANUAL_REVIEW, truncate(error), null));
            return;
        }
        Instant nextRetryAt = Instant.now().plus(compensationBackoff(current.attempts()));
        OrderCreateSaga compensating =
                stateService.save(current.status(OrderSagaStatus.COMPENSATING, truncate(error), nextRetryAt));
        boolean inventoryReleased = releaseInventory(compensating);
        boolean couponReleased = releaseCoupon(compensating);
        OrderCreateSaga latest = stateService.require(requestId);
        if (inventoryReleased && couponReleased) {
            stateService.save(latest.status(OrderSagaStatus.COMPENSATED, truncate(error), null));
            return;
        }
        Instant retryAt = latest.attempts() >= MAXIMUM_COMPENSATION_ATTEMPTS
                ? null
                : Instant.now().plus(compensationBackoff(latest.attempts()));
        stateService.save(latest.status(OrderSagaStatus.MANUAL_REVIEW,
                truncate(error + "; inventoryReleased=" + inventoryReleased + "; couponReleased=" + couponReleased),
                retryAt));
    }

    private boolean releaseInventory(OrderCreateSaga saga) {
        if (saga.stage().ordinal() < OrderSagaStage.INVENTORY_RESERVING.ordinal()
                || saga.stage() == OrderSagaStage.RESOURCES_RELEASED) {
            return true;
        }
        try {
            InventoryClient.InventoryReservation reservation = inventoryClient.release(saga.inventoryReservationId());
            return reservation != null && reservation.released();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean releaseCoupon(OrderCreateSaga saga) {
        if (saga.stage().ordinal() < OrderSagaStage.COUPON_RESERVING.ordinal() || saga.couponId() == null
                || saga.couponId().isBlank()) {
            return true;
        }
        try {
            return marketingClient.releaseCoupon(saga.requestId(), saga.couponId(), saga.orderId());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Duration compensationBackoff(int attempts) {
        return Duration.ofSeconds(Math.min(300L, 1L << Math.min(attempts + 1, 8)));
    }

    private String truncate(String value) {
        String safe = Objects.toString(value, "unknown saga failure");
        return safe.substring(0, Math.min(500, safe.length()));
    }
}
