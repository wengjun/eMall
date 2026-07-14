package com.emall.order.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.exception.BusinessException;
import com.emall.common.rpc.OrderPaymentConfirmationCommand;
import com.emall.common.sharding.ShardRouteIndex;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientType;
import com.emall.order.domain.OrderStatus;
import com.emall.order.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderPaymentConfirmationServiceTest {
    private final OrderService orderService = mock(OrderService.class);
    private final OrderPaymentConfirmationService service = new OrderPaymentConfirmationService(orderService,
            new InMemoryOrderPaymentConfirmationRepository(), ShardRoutingOperations.noop(), ShardRouteIndex.local());

    @Test
    void bindsOrderToOneExactPaymentAndTreatsExactRetryAsIdempotent() {
        when(orderService.get(1001L)).thenReturn(order(OrderStatus.CREATED), order(OrderStatus.PAID));
        OrderPaymentConfirmationCommand command = command(2001L, new BigDecimal("99.00"), "trade-1");

        assertThat(service.confirm(command)).isTrue();
        assertThat(service.confirm(command)).isTrue();

        verify(orderService).pay(1001L);
    }

    @Test
    void rejectsAmountMismatchAndDifferentPaymentRebinding() {
        when(orderService.get(1001L)).thenReturn(order(OrderStatus.CREATED));

        assertThat(service.confirm(command(2001L, new BigDecimal("98.00"), "trade-1"))).isFalse();
        assertThat(service.confirm(command(2001L, new BigDecimal("99.00"), "trade-1"))).isTrue();
        assertThatThrownBy(() -> service.confirm(command(2002L, new BigDecimal("99.00"), "trade-2")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("another payment");
    }

    private OrderPaymentConfirmationCommand command(long paymentId, BigDecimal amount, String tradeNo) {
        return new OrderPaymentConfirmationCommand(1001L, paymentId, amount, "CNY", tradeNo);
    }

    private Order order(OrderStatus status) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new Order(1001L, "request-1", 3001L, 4001L, 1, OrderClientType.WEB, "device", "web",
                new BigDecimal("99.00"), new BigDecimal("99.00"), BigDecimal.ZERO, new BigDecimal("99.00"), "CNY", 1L,
                null, "reservation-1", status, null, now, now);
    }
}
