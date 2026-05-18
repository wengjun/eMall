package com.emall.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientContext;
import com.emall.order.domain.OrderClientType;
import com.emall.order.domain.OrderStatus;
import com.emall.order.integration.InventoryClient;
import com.emall.order.integration.InventoryClient.InventoryReservation;
import com.emall.order.integration.InventoryClient.ReserveInventoryRequest;
import com.emall.order.integration.MarketingClient;
import com.emall.order.integration.MarketingClient.PromotionQuote;
import com.emall.order.integration.PricingClient;
import com.emall.order.integration.PricingClient.PriceQuote;
import com.emall.order.repository.InMemoryOrderRepository;
import com.emall.order.repository.InMemoryOutboxRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderServiceTest {
    private final InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
    private final FakeInventoryClient inventoryClient = new FakeInventoryClient();
    private final OrderService orderService = new OrderService(new InMemoryOrderRepository(), outboxRepository,
            new SnowflakeIdGenerator(3), inventoryClient, new FakePricingClient(), new FakeMarketingClient());

    @Test
    void shouldCreateAndPayOrderWithDownstreamClients() {
        Order created = orderService.create("order-request-001", 70001L, 30001L, 2);
        Order duplicate = orderService.create("order-request-001", 70001L, 30001L, 2);
        Order paid = orderService.pay(created.orderId());

        assertThat(duplicate.orderId()).isEqualTo(created.orderId());
        assertThat(created.clientType()).isEqualTo(OrderClientType.WEB);
        assertThat(created.deviceId()).isEqualTo(OrderClientContext.UNKNOWN_DEVICE);
        assertThat(created.channel()).isEqualTo(OrderClientContext.DIRECT_CHANNEL);
        assertThat(created.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.payableAmount()).isEqualByComparingTo("190.00");
        assertThat(inventoryClient.confirmedRequestId).isEqualTo(created.inventoryReservationId());
        assertThat(outboxRepository.findPublishable(Instant.now(), 10)).hasSize(2);
        assertThat(outboxRepository.findPublishable(Instant.now(), 10))
                .extracting(event -> event.payload().get("clientType")).containsOnly("WEB");
        assertThat(outboxRepository.findPublishable(Instant.now(), 10))
                .extracting(event -> event.payload().get("deviceId"))
                .containsOnly(OrderClientContext.UNKNOWN_DEVICE);
    }

    @Test
    void shouldCreateAppOrderWithClientTypeInEventPayload() {
        OrderClientContext context = OrderClientContext.of(OrderClientType.APP, "ios-device-001", "ios-app");
        Order created = orderService.create("app-order-request-001", 70001L, 30001L, 1, context);

        assertThat(created.clientType()).isEqualTo(OrderClientType.APP);
        assertThat(created.deviceId()).isEqualTo("ios-device-001");
        assertThat(created.channel()).isEqualTo("ios-app");
        assertThat(outboxRepository.findPublishable(Instant.now(), 10))
                .anySatisfy(event -> assertThat(event.payload()).containsEntry("clientType", "APP")
                        .containsEntry("deviceId", "ios-device-001").containsEntry("channel", "ios-app"));
    }

    private static final class FakeInventoryClient extends InventoryClient {
        private String confirmedRequestId;

        private FakeInventoryClient() {
            super(null, null);
        }

        @Override
        public InventoryReservation reserve(ReserveInventoryRequest request) {
            Instant now = Instant.now();
            return new InventoryReservation(request.requestId(), request.skuId(), request.quantity(), "RESERVED", null,
                    now.plusSeconds(900), now, now);
        }

        @Override
        public InventoryReservation confirm(String requestId) {
            confirmedRequestId = requestId;
            Instant now = Instant.now();
            return new InventoryReservation(requestId, 30001L, 2, "CONFIRMED", null, now, now, now);
        }
    }

    private static final class FakePricingClient extends PricingClient {
        private FakePricingClient() {
            super(null);
        }

        @Override
        public PriceQuote quote(long skuId, int quantity) {
            BigDecimal unitPrice = new BigDecimal("100.00");
            return new PriceQuote(skuId, unitPrice, quantity, unitPrice.multiply(BigDecimal.valueOf(quantity)), "CNY",
                    1L, Instant.now());
        }
    }

    private static final class FakeMarketingClient extends MarketingClient {
        private FakeMarketingClient() {
            super(null);
        }

        @Override
        public PromotionQuote quote(long userId, BigDecimal orderAmount) {
            return new PromotionQuote(userId, orderAmount, new BigDecimal("10.00"),
                    orderAmount.subtract(new BigDecimal("10.00")), "coupon-001", Instant.now());
        }
    }
}
