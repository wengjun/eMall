package com.emall.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.event.EventTypes;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.order.domain.Order;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class OrderConcurrencyTest {
    @Test
    void shouldPublishPaidEventOnceWhenPayRequestsRace() throws Exception {
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        OrderService orderService =
                new OrderService(new InMemoryOrderRepository(), outboxRepository, new SnowflakeIdGenerator(3),
                        new AlwaysConfirmInventoryClient(), new FixedPricingClient(), new NoDiscountMarketingClient());
        Order order = orderService.create("order-race-001", 70001L, 30001L, 1);

        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Order>> tasks = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            tasks.add(() -> {
                start.await();
                return orderService.pay(order.orderId());
            });
        }

        List<Future<Order>> futures = tasks.stream().map(executor::submit).toList();
        start.countDown();
        for (Future<Order> future : futures) {
            future.get();
        }
        executor.shutdownNow();

        long paidEvents = outboxRepository.findPublishable(Instant.now(), 100).stream()
                .filter(event -> EventTypes.ORDER_PAID.equals(event.eventType())).count();
        assertThat(paidEvents).isEqualTo(1);
    }

    private static final class AlwaysConfirmInventoryClient extends InventoryClient {
        private AlwaysConfirmInventoryClient() {
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
            Instant now = Instant.now();
            return new InventoryReservation(requestId, 30001L, 1, "CONFIRMED", null, now, now, now);
        }
    }

    private static final class FixedPricingClient extends PricingClient {
        private FixedPricingClient() {
            super(null);
        }

        @Override
        public PriceQuote quote(long skuId, int quantity) {
            BigDecimal unitPrice = new BigDecimal("100.00");
            return new PriceQuote(skuId, unitPrice, quantity, unitPrice.multiply(BigDecimal.valueOf(quantity)), "CNY",
                    1L, Instant.now());
        }
    }

    private static final class NoDiscountMarketingClient extends MarketingClient {
        private NoDiscountMarketingClient() {
            super(null);
        }

        @Override
        public PromotionQuote quote(long userId, BigDecimal orderAmount) {
            return new PromotionQuote(userId, orderAmount, BigDecimal.ZERO, orderAmount, null, Instant.now());
        }
    }
}
