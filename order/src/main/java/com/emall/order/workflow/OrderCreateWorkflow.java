package com.emall.order.workflow;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.outbox.OutboxRepository;
import com.emall.common.trust.ClientTrustContext;
import com.emall.common.trust.RiskEvaluationRequest;
import com.emall.common.trust.RiskGuard;
import com.emall.common.trust.RiskScene;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientContext;
import com.emall.order.domain.OrderStatus;
import com.emall.order.integration.InventoryClient;
import com.emall.order.integration.InventoryClient.InventoryReservation;
import com.emall.order.integration.InventoryClient.ReserveInventoryRequest;
import com.emall.order.integration.MarketingClient;
import com.emall.order.integration.MarketingClient.CouponReservation;
import com.emall.order.integration.MarketingClient.PromotionQuote;
import com.emall.order.integration.PricingClient;
import com.emall.order.integration.PricingClient.PriceQuote;
import com.emall.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class OrderCreateWorkflow {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final InventoryClient inventoryClient;
    private final PricingClient pricingClient;
    private final MarketingClient marketingClient;
    private final BusinessMetrics businessMetrics;
    private final RiskGuard riskGuard;

    public OrderCreateWorkflow(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient, BusinessMetrics businessMetrics, RiskGuard riskGuard) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.inventoryClient = inventoryClient;
        this.pricingClient = pricingClient;
        this.marketingClient = marketingClient;
        this.businessMetrics = businessMetrics;
        this.riskGuard = riskGuard;
    }

    public Order create(String requestId, long userId, long skuId, int quantity, OrderClientContext clientContext,
            ClientTrustContext trustContext) {
        long orderId = idGenerator.nextId();
        PriceQuote priceQuote = pricingClient.quote(skuId, quantity);
        PromotionQuote quotedPromotion = marketingClient.quote(userId, priceQuote.subtotal());
        validatePayableAmount(priceQuote, quotedPromotion);
        CouponReservation couponReservation = marketingClient.reserveCoupon(requestId, userId,
                quotedPromotion.couponId(), quotedPromotion.orderAmount(), orderId);
        PromotionQuote promotionQuote =
                couponReservation.reserved() ? quotedPromotion : PromotionQuote.none(userId, priceQuote.subtotal());
        riskGuard.check(new RiskEvaluationRequest(RiskScene.ORDER_CREATE, trustContext.subjectId(userId),
                trustContext.deviceId(), trustContext.sourceIp(), promotionQuote.payableAmount(), quantity));
        InventoryReservation reservation =
                inventoryClient.reserve(new ReserveInventoryRequest(requestId, skuId, quantity));
        if (!reservation.reserved() && couponReservation.reserved()) {
            marketingClient.releaseCoupon(requestId, couponReservation.couponId(), orderId);
            promotionQuote = PromotionQuote.none(userId, priceQuote.subtotal());
        }
        Instant now = Instant.now();
        OrderStatus status = reservation.reserved() ? OrderStatus.CREATED : OrderStatus.PENDING_RETRY;
        String reason = reservation.reserved() ? null : reservation.reason();
        Order order = orderRepository.save(new Order(orderId, requestId, userId, skuId, quantity,
                clientContext.clientType(), clientContext.deviceId(), clientContext.channel(), priceQuote.unitPrice(),
                priceQuote.subtotal(), promotionQuote.discountAmount(), promotionQuote.payableAmount(),
                priceQuote.currency(), priceQuote.priceVersion(), promotionQuote.couponId(), requestId, status, reason,
                now, now));
        orderRepository.saveRoute(order.orderId(), order.requestId(), order.userId());
        OrderCreateContext context = new OrderCreateContext(requestId, userId, skuId, quantity, clientContext,
                trustContext, priceQuote, promotionQuote, couponReservation, reservation, order);
        if (context.order().status() == OrderStatus.CREATED) {
            appendEvent(context.order(), EventTypes.ORDER_CREATED);
            businessMetrics.increment(BusinessMetricNames.ORDER_CREATED, "client_type", order.clientType().name(),
                    "channel", order.channel());
        } else {
            businessMetrics.increment(BusinessMetricNames.ORDER_PENDING_RETRY, "reason",
                    reason == null ? "unknown" : reason);
        }
        return context.order();
    }

    private void appendEvent(Order order, String eventType) {
        outboxRepository.save(OutboxEvent.create("order-event-" + idGenerator.nextId(), "Order",
                String.valueOf(order.orderId()), eventType,
                Map.ofEntries(Map.entry("orderId", order.orderId()), Map.entry("userId", order.userId()),
                        Map.entry("skuId", order.skuId()), Map.entry("quantity", order.quantity()),
                        Map.entry("clientType", order.clientType().name()), Map.entry("deviceId", order.deviceId()),
                        Map.entry("channel", order.channel()), Map.entry("unitPrice", order.unitPrice()),
                        Map.entry("subtotalAmount", order.subtotalAmount()),
                        Map.entry("discountAmount", order.discountAmount()),
                        Map.entry("payableAmount", order.payableAmount()), Map.entry("currency", order.currency()),
                        Map.entry("priceVersion", order.priceVersion()),
                        Map.entry("couponId", order.couponId() == null ? "" : order.couponId()),
                        Map.entry("inventoryReservationId", order.inventoryReservationId()),
                        Map.entry("status", order.status().name()))));
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
