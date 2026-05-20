package com.emall.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emall.common.api.ApiResponse;
import com.emall.order.api.OrderController.CreateOrderRequest;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientContext;
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
        Order order =
                newOrder(OrderClientType.WEB, OrderClientContext.UNKNOWN_DEVICE, OrderClientContext.DIRECT_CHANNEL);
        OrderClientContext expectedContext = OrderClientContext.webDefault();
        when(orderService.create("legacy-web-request", 70001L, 30001L, 1, expectedContext)).thenReturn(order);

        ApiResponse<Order> response = controller.createOrder(
                new CreateOrderRequest("legacy-web-request", 70001L, 30001L, 1, null, null, null), null, null);

        assertThat(response.data().clientType()).isEqualTo(OrderClientType.WEB);
        assertThat(response.data().deviceId()).isEqualTo(OrderClientContext.UNKNOWN_DEVICE);
        assertThat(response.data().channel()).isEqualTo(OrderClientContext.DIRECT_CHANNEL);
        verify(orderService).create("legacy-web-request", 70001L, 30001L, 1, expectedContext);
    }

    @Test
    void shouldForwardAppClientTypeToOrderService() {
        Order order = newOrder(OrderClientType.APP, "ios-device-001", "jd-app");
        OrderClientContext expectedContext = OrderClientContext.of(OrderClientType.APP, "ios-device-001", "jd-app");
        when(orderService.create("app-request", 70001L, 30001L, 1, expectedContext)).thenReturn(order);

        ApiResponse<Order> response = controller.createOrder(new CreateOrderRequest("app-request", 70001L, 30001L, 1,
                OrderClientType.APP, "ios-device-001", "jd-app"), null, null);

        assertThat(response.data().clientType()).isEqualTo(OrderClientType.APP);
        assertThat(response.data().deviceId()).isEqualTo("ios-device-001");
        assertThat(response.data().channel()).isEqualTo("jd-app");
        verify(orderService).create("app-request", 70001L, 30001L, 1, expectedContext);
    }

    @Test
    void shouldUseClientContextHeadersWhenBodyDoesNotContainThem() {
        Order order = newOrder(OrderClientType.WEB, "web-device-001", "pc-web");
        OrderClientContext expectedContext = OrderClientContext.of(OrderClientType.WEB, "web-device-001", "pc-web");
        when(orderService.create("web-request", 70001L, 30001L, 1, expectedContext)).thenReturn(order);

        ApiResponse<Order> response = controller.createOrder(
                new CreateOrderRequest("web-request", 70001L, 30001L, 1, OrderClientType.WEB, null, null),
                "web-device-001", "pc-web");

        assertThat(response.data().deviceId()).isEqualTo("web-device-001");
        assertThat(response.data().channel()).isEqualTo("pc-web");
        verify(orderService).create("web-request", 70001L, 30001L, 1, expectedContext);
    }

    private static Order newOrder(OrderClientType clientType, String deviceId, String channel) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new Order(10001L, "request-001", 70001L, 30001L, 1, clientType, deviceId, channel,
                new BigDecimal("100.00"), new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"), "CNY",
                1L, null, "order-10001", OrderStatus.CREATED, null, now, now);
    }
}
