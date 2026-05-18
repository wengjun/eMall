package com.emall.order.service;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.outbox.OutboxRepository;
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
import com.emall.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final InventoryClient inventoryClient;
    private final PricingClient pricingClient;
    private final MarketingClient marketingClient;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.inventoryClient = inventoryClient;
        this.pricingClient = pricingClient;
        this.marketingClient = marketingClient;
    }

    @Transactional
    public synchronized Order create(String requestId, long userId, long skuId, int quantity) {
        return create(requestId, userId, skuId, quantity, OrderClientContext.webDefault());
    }

    @Transactional
    public synchronized Order create(String requestId, long userId, long skuId, int quantity,
            OrderClientType clientType) {
        return create(requestId, userId, skuId, quantity,
                OrderClientContext.of(clientType, OrderClientContext.UNKNOWN_DEVICE,
                        OrderClientContext.DIRECT_CHANNEL));
    }

    @Transactional
    public synchronized Order create(String requestId, long userId, long skuId, int quantity,
            OrderClientContext clientContext) {
        return orderRepository.findByRequestId(requestId)
                .orElseGet(() -> createOnce(requestId, userId, skuId, quantity,
                        clientContext == null ? OrderClientContext.webDefault() : clientContext));
    }

    public Order get(long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order not found"));
    }

    public List<Order> findByStatus(OrderStatus status, int limit) {
        return orderRepository.findByStatus(status, limit);
    }

    @Transactional
    public synchronized Order pay(long orderId) {
        Order order = get(orderId);
        if (order.status() == OrderStatus.PAID) {
            return order;
        }
        if (order.status() != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid from " + order.status());
        }
        InventoryReservation reservation = inventoryClient.confirm(order.inventoryReservationId());
        if (reservation == null || !reservation.confirmed()) {
            return orderRepository.save(order.markPendingRetry("inventory confirm pending"));
        }
        Order paid = orderRepository.save(order.markPaid());
        appendEvent(paid, EventTypes.ORDER_PAID);
        return paid;
    }

    @Transactional
    public synchronized Order cancel(long orderId) {
        Order order = get(orderId);
        if (order.status() == OrderStatus.CANCELLED || order.status() == OrderStatus.CLOSED) {
            return order;
        }
        if (order.status() == OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.CONFLICT, "paid order requires refund flow");
        }
        InventoryReservation reservation = inventoryClient.release(order.inventoryReservationId());
        if (reservation == null || !reservation.released()) {
            return orderRepository.save(order.markPendingRetry("inventory release pending"));
        }
        Order cancelled = orderRepository.save(order.markCancelled());
        appendEvent(cancelled, EventTypes.ORDER_CANCELLED);
        return cancelled;
    }

    @Transactional
    public synchronized Order retryPending(long orderId) {
        Order order = get(orderId);
        if (order.status() != OrderStatus.PENDING_RETRY) {
            return order;
        }
        String reason = order.failureReason() == null ? "" : order.failureReason();
        if (reason.contains("confirm")) {
            return payAfterRetry(order);
        }
        if (reason.contains("release")) {
            return cancelAfterRetry(order);
        }
        InventoryReservation reservation = inventoryClient
                .reserve(new ReserveInventoryRequest(order.inventoryReservationId(), order.skuId(), order.quantity()));
        if (reservation.reserved()) {
            Order created = orderRepository.save(order.markCreated());
            appendEvent(created, EventTypes.ORDER_CREATED);
            return created;
        }
        return orderRepository.save(order.markPendingRetry(reservation.reason()));
    }

    private Order createOnce(String requestId, long userId, long skuId, int quantity,
            OrderClientContext clientContext) {
        long orderId = idGenerator.nextId();
        String reservationId = "order-" + orderId;
        PriceQuote priceQuote = pricingClient.quote(skuId, quantity);
        PromotionQuote promotionQuote = marketingClient.quote(userId, priceQuote.subtotal());
        validatePayableAmount(priceQuote, promotionQuote);
        InventoryReservation reservation =
                inventoryClient.reserve(new ReserveInventoryRequest(reservationId, skuId, quantity));
        Instant now = Instant.now();
        OrderStatus status = reservation.reserved() ? OrderStatus.CREATED : OrderStatus.PENDING_RETRY;
        String reason = reservation.reserved() ? null : reservation.reason();
        Order order = orderRepository.save(
                new Order(orderId, requestId, userId, skuId, quantity, clientContext.clientType(),
                        clientContext.deviceId(), clientContext.channel(), priceQuote.unitPrice(),
                        priceQuote.subtotal(), promotionQuote.discountAmount(), promotionQuote.payableAmount(),
                        priceQuote.currency(), priceQuote.priceVersion(), promotionQuote.couponId(), reservationId,
                        status, reason, now, now));
        if (order.status() == OrderStatus.CREATED) {
            appendEvent(order, EventTypes.ORDER_CREATED);
        }
        return order;
    }

    private Order payAfterRetry(Order order) {
        InventoryReservation reservation = inventoryClient.confirm(order.inventoryReservationId());
        if (reservation != null && reservation.confirmed()) {
            Order paid = orderRepository.save(order.markPaid());
            appendEvent(paid, EventTypes.ORDER_PAID);
            return paid;
        }
        return order;
    }

    private Order cancelAfterRetry(Order order) {
        InventoryReservation reservation = inventoryClient.release(order.inventoryReservationId());
        if (reservation != null && reservation.released()) {
            Order cancelled = orderRepository.save(order.markCancelled());
            appendEvent(cancelled, EventTypes.ORDER_CANCELLED);
            return cancelled;
        }
        return order;
    }

    private void appendEvent(Order order, String eventType) {
        outboxRepository.save(OutboxEvent.create("order-event-" + idGenerator.nextId(), "Order",
                String.valueOf(order.orderId()), eventType,
                Map.ofEntries(Map.entry("orderId", order.orderId()), Map.entry("userId", order.userId()),
                        Map.entry("skuId", order.skuId()), Map.entry("quantity", order.quantity()),
                        Map.entry("clientType", order.clientType().name()),
                        Map.entry("deviceId", order.deviceId()), Map.entry("channel", order.channel()),
                        Map.entry("unitPrice", order.unitPrice()), Map.entry("subtotalAmount", order.subtotalAmount()),
                        Map.entry("discountAmount", order.discountAmount()),
                        Map.entry("payableAmount", order.payableAmount()), Map.entry("currency", order.currency()),
                        Map.entry("priceVersion", order.priceVersion()), Map.entry("status", order.status().name()))));
    }

    private void validatePayableAmount(PriceQuote priceQuote, PromotionQuote promotionQuote) {
        if (promotionQuote.payableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "payable amount must be positive");
        }
        if (promotionQuote.payableAmount().compareTo(priceQuote.subtotal()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "payable amount cannot exceed subtotal");
        }
    }
}
