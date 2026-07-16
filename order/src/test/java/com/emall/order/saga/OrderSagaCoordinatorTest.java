package com.emall.order.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.order.integration.InventoryClient;
import com.emall.order.integration.MarketingClient;
import com.emall.order.repository.InMemoryOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderSagaCoordinatorTest {
    private final InMemoryOrderSagaRepository repository = new InMemoryOrderSagaRepository();
    private final InventoryClient inventoryClient = mock(InventoryClient.class);
    private final MarketingClient marketingClient = mock(MarketingClient.class);
    private final OrderSagaCoordinator coordinator = new OrderSagaCoordinator(new OrderSagaStateService(repository),
            new InMemoryOrderRepository(), inventoryClient, marketingClient, ShardRoutingOperations.noop());

    @BeforeEach
    void configureIdempotentCompensations() {
        when(inventoryClient.getReservation(anyString())).thenAnswer(invocation -> {
            String requestId = invocation.getArgument(0);
            return new InventoryClient.InventoryReservation(requestId, 3001L, 1, "RESERVED", null, null, null, null);
        });
        when(inventoryClient.release(anyString())).thenReturn(
                new InventoryClient.InventoryReservation("request-1", 3001L, 1, "RELEASED", null, null, null, null));
        when(marketingClient.getCoupon(anyString())).thenAnswer(invocation -> {
            String couponId = invocation.getArgument(0);
            int suffix = Integer.parseInt(couponId.substring(couponId.lastIndexOf('-') + 1));
            return new MarketingClient.CouponReservation("request-" + suffix, 2000L + suffix, couponId, "RESERVED",
                    BigDecimal.TEN, 1000L + suffix, Instant.now(), null);
        });
        when(marketingClient.releaseCoupon(anyString(), anyString(), anyLong())).thenReturn(true);
    }

    @Test
    void compensatesUncertainInventoryAndCouponReservationsAfterRollback() {
        OrderCreateSaga saga = coordinator.start(1L, "request-1", 1001L, 2001L, 3001L);
        saga = coordinator.advance(saga, OrderSagaStage.INVENTORY_RESERVING, "coupon-1", "request-1");

        coordinator.compensateAfterRollback(saga, "order transaction rolled back");

        OrderCreateSaga compensated = repository.findByRequestId("request-1").orElseThrow();
        assertThat(compensated.status()).isEqualTo(OrderSagaStatus.COMPENSATED);
        verify(inventoryClient).release("request-1");
        verify(marketingClient).releaseCoupon("request-1", "coupon-1", 1001L);
    }

    @Test
    void retriesFailedCompensationUntilResourcesConverge() {
        when(inventoryClient.release("request-2")).thenThrow(new IllegalStateException("inventory timeout")).thenReturn(
                new InventoryClient.InventoryReservation("request-2", 3002L, 1, "RELEASED", null, null, null, null));
        OrderCreateSaga saga = coordinator.start(2L, "request-2", 1002L, 2002L, 3002L);
        saga = coordinator.advance(saga, OrderSagaStage.INVENTORY_RESERVED, "coupon-2", "request-2");

        coordinator.compensateAfterRollback(saga, "database failure");
        OrderCreateSaga retryable = repository.findByRequestId("request-2").orElseThrow();
        coordinator.recover(retryable);

        assertThat(repository.findByRequestId("request-2").orElseThrow().status())
                .isEqualTo(OrderSagaStatus.COMPENSATED);
    }

    @Test
    void completesPersistedSagaWithoutCompensationAfterCommit() {
        OrderCreateSaga saga = coordinator.start(3L, "request-3", 1003L, 2003L, 3003L);
        saga = coordinator.advance(saga, OrderSagaStage.ORDER_PERSISTED, "coupon-3", "request-3");

        coordinator.completeAfterCommit(saga);

        assertThat(repository.findByRequestId("request-3").orElseThrow().status()).isEqualTo(OrderSagaStatus.COMPLETED);
        verify(inventoryClient, never()).release("request-3");
    }

    @Test
    void skipsRemoteCompensationBeforeAnyReservationWasAttempted() {
        OrderCreateSaga saga = coordinator.start(4L, "request-4", 1004L, 2004L, 3004L);

        coordinator.compensateAfterRollback(saga, "validation failure");

        assertThat(repository.findByRequestId("request-4").orElseThrow().status())
                .isEqualTo(OrderSagaStatus.COMPENSATED);
        verify(inventoryClient, never()).release(anyString());
        verify(marketingClient, never()).releaseCoupon(anyString(), anyString(), anyLong());
    }

    @Test
    void compensatesUncertainCouponWithoutReleasingInventoryBeforeInventoryAttempt() {
        OrderCreateSaga saga = coordinator.start(5L, "request-5", 1005L, 2005L, 3005L);
        saga = coordinator.advance(saga, OrderSagaStage.COUPON_RESERVING, "coupon-5", "request-5");

        coordinator.compensateAfterRollback(saga, "coupon response timeout");

        assertThat(repository.findByRequestId("request-5").orElseThrow().status())
                .isEqualTo(OrderSagaStatus.COMPENSATED);
        verify(inventoryClient, never()).release("request-5");
        verify(marketingClient).releaseCoupon("request-5", "coupon-5", 1005L);
    }

    @Test
    void reusesOriginalOrderIdWhenRetryingACompensatedRequest() {
        OrderCreateSaga original = coordinator.start(6L, "request-6", 1006L, 2006L, 3006L);
        coordinator.compensateAfterRollback(original, "transient failure");

        OrderCreateSaga restarted = coordinator.start(7L, "request-6", 1007L, 2006L, 3006L);

        assertThat(restarted.orderId()).isEqualTo(1006L);
        assertThat(restarted.status()).isEqualTo(OrderSagaStatus.RUNNING);
        assertThat(restarted.stage()).isEqualTo(OrderSagaStage.STARTED);
    }

    @Test
    void doesNotBlindlyReleaseInventoryWhenReservationStateCannotBeConfirmed() {
        OrderCreateSaga saga = coordinator.start(7L, "request-7", 1007L, 2007L, 3007L);
        saga = coordinator.advance(saga, OrderSagaStage.INVENTORY_RESERVING, null, "request-7");
        when(inventoryClient.getReservation("request-7")).thenThrow(new IllegalStateException("query timeout"));

        coordinator.compensateAfterRollback(saga, "reserve response timeout");

        assertThat(repository.findByRequestId("request-7").orElseThrow().status())
                .isEqualTo(OrderSagaStatus.MANUAL_REVIEW);
        verify(inventoryClient, never()).release("request-7");
    }
}
