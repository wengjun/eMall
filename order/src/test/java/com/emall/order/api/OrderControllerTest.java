package com.emall.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.api.ApiResponse;
import com.emall.order.api.OrderController.CreateOrderRequest;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientType;
import com.emall.order.domain.OrderStatus;
import com.emall.order.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderControllerTest {
    private final OrderService orderService = mock(OrderService.class);
    private final OrderController controller = new OrderController(orderService);

    @Test
    void shouldDefaultMissingClientTypeToWebForLegacyClients() {
        Order order = newOrder(OrderClientType.WEB);
        when(orderService.create("legacy-web-request", 70001L, 30001L, 1, OrderClientType.WEB)).thenReturn(order);

        ApiResponse<Order> response =
                controller.createOrder(new CreateOrderRequest("legacy-web-request", 70001L, 30001L, 1, null));

        assertThat(response.data().clientType()).isEqualTo(OrderClientType.WEB);
        verify(orderService).create("legacy-web-request", 70001L, 30001L, 1, OrderClientType.WEB);
    }

    @Test
    void shouldForwardAppClientTypeToOrderService() {
        Order order = newOrder(OrderClientType.APP);
        when(orderService.create("app-request", 70001L, 30001L, 1, OrderClientType.APP)).thenReturn(order);

        ApiResponse<Order> response =
                controller.createOrder(new CreateOrderRequest("app-request", 70001L, 30001L, 1, OrderClientType.APP));

        assertThat(response.data().clientType()).isEqualTo(OrderClientType.APP);
        verify(orderService).create("app-request", 70001L, 30001L, 1, OrderClientType.APP);
    }

    private static Order newOrder(OrderClientType clientType) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new Order(10001L, "request-001", 70001L, 30001L, 1, clientType, new BigDecimal("100.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), "CNY", 1L, null,
                "order-10001", OrderStatus.CREATED, null, now, now);
    }
}
