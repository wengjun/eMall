package com.emall.order.workflow;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
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
import com.emall.order.saga.OrderCreateSaga;
import com.emall.order.saga.OrderSagaCoordinator;
import com.emall.order.saga.OrderSagaStage;
import com.emall.order.transaction.OrderLocalTransaction;
import java.math.BigDecimal;
import java.time.Instant;

public class OrderCreateWorkflow {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final InventoryClient inventoryClient;
    private final PricingClient pricingClient;
    private final MarketingClient marketingClient;
    private final BusinessMetrics businessMetrics;
    private final RiskGuard riskGuard;
    private final OrderSagaCoordinator sagaCoordinator;
    private final OrderLocalTransaction localTransaction;

    public OrderCreateWorkflow(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient, BusinessMetrics businessMetrics, RiskGuard riskGuard,
            OrderSagaCoordinator sagaCoordinator, OrderLocalTransaction localTransaction) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.inventoryClient = inventoryClient;
        this.pricingClient = pricingClient;
        this.marketingClient = marketingClient;
        this.businessMetrics = businessMetrics;
        this.riskGuard = riskGuard;
        this.sagaCoordinator = sagaCoordinator;
        this.localTransaction = localTransaction;
    }

    public Order create(String requestId, long userId, long skuId, int quantity, OrderClientContext clientContext,
            ClientTrustContext trustContext) {
        long proposedOrderId = idGenerator.nextId();
        OrderCreateSaga saga = sagaCoordinator.start(proposedOrderId, requestId, proposedOrderId, userId, skuId);
        long orderId = saga.orderId();
        try {
            PriceQuote priceQuote = pricingClient.quote(skuId, quantity);
            PromotionQuote quotedPromotion = marketingClient.quote(userId, priceQuote.subtotal());
            validatePayableAmount(priceQuote, quotedPromotion);
            riskGuard.check(new RiskEvaluationRequest(RiskScene.ORDER_CREATE, trustContext.subjectId(userId),
                    trustContext.deviceId(), trustContext.sourceIp(), quotedPromotion.payableAmount(), quantity));
            saga = sagaCoordinator.advance(saga, OrderSagaStage.VALIDATED, null, requestId);
            saga = sagaCoordinator.advance(saga, OrderSagaStage.COUPON_PLANNED, quotedPromotion.couponId(), requestId);
            saga = sagaCoordinator.advance(saga, OrderSagaStage.COUPON_RESERVING, quotedPromotion.couponId(),
                    requestId);
            CouponReservation couponReservation = marketingClient.reserveCoupon(requestId, userId,
                    quotedPromotion.couponId(), quotedPromotion.orderAmount(), orderId);
            PromotionQuote promotionQuote =
                    couponReservation.reserved() ? quotedPromotion : PromotionQuote.none(userId, priceQuote.subtotal());
            if (couponReservation.reserved()) {
                saga = sagaCoordinator.advance(saga, OrderSagaStage.COUPON_RESERVED, couponReservation.couponId(),
                        requestId);
            } else {
                saga = sagaCoordinator.advance(saga, OrderSagaStage.COUPON_RESOLVED, "", requestId);
            }
            saga = sagaCoordinator.advance(saga, OrderSagaStage.INVENTORY_RESERVING, null, requestId);
            InventoryReservation reservation =
                    inventoryClient.reserve(new ReserveInventoryRequest(requestId, skuId, quantity));
            if (reservation.reserved()) {
                saga = sagaCoordinator.advance(saga, OrderSagaStage.INVENTORY_RESERVED, promotionQuote.couponId(),
                        reservation.requestId());
            } else if (couponReservation.reserved()) {
                boolean released = marketingClient.releaseCoupon(requestId, couponReservation.couponId(), orderId);
                if (!released) {
                    throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE,
                            "coupon compensation could not be confirmed");
                }
                promotionQuote = PromotionQuote.none(userId, priceQuote.subtotal());
            }
            if (!reservation.reserved()) {
                saga = sagaCoordinator.advance(saga, OrderSagaStage.RESOURCES_RELEASED, "", requestId);
            }
            Instant now = Instant.now();
            OrderStatus status = reservation.reserved() ? OrderStatus.CREATED : OrderStatus.PENDING_RETRY;
            String reason = reservation.reserved() ? null : reservation.reason();
            PromotionQuote finalPromotionQuote = promotionQuote;
            Order order = localTransaction.execute("create", () -> {
                Order saved = orderRepository.save(new Order(orderId, requestId, userId, skuId, quantity,
                        clientContext.clientType(), clientContext.deviceId(), clientContext.channel(),
                        priceQuote.unitPrice(), priceQuote.subtotal(), finalPromotionQuote.discountAmount(),
                        finalPromotionQuote.payableAmount(), priceQuote.currency(), priceQuote.priceVersion(),
                        finalPromotionQuote.couponId(), requestId, status, reason, now, now));
                orderRepository.saveRoute(saved.orderId(), saved.requestId(), saved.userId());
                if (saved.status() == OrderStatus.CREATED) {
                    appendEvent(saved, EventTypes.ORDER_CREATED);
                }
                return saved;
            });
            saga = sagaCoordinator.advance(saga, OrderSagaStage.ORDER_PERSISTED, promotionQuote.couponId(),
                    reservation.requestId());
            if (order.status() == OrderStatus.CREATED) {
                businessMetrics.increment(BusinessMetricNames.ORDER_CREATED, "client_type", order.clientType().name(),
                        "channel", order.channel());
            } else {
                businessMetrics.increment(BusinessMetricNames.ORDER_PENDING_RETRY, "reason",
                        reason == null ? "unknown" : reason);
            }
            sagaCoordinator.completeAfterCommit(saga);
            return order;
        } catch (RuntimeException ex) {
            sagaCoordinator.compensateAfterRollback(saga, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }

    private void appendEvent(Order order, String eventType) {
        outboxRepository.save(OutboxEvent.create("order-event-" + idGenerator.nextId(), "Order",
                String.valueOf(order.orderId()), eventType, "order", "0.1.0",
                new OrderEventPayload(order.orderId(), order.userId(), order.skuId(), order.quantity(),
                        order.clientType().name(), order.deviceId(), order.channel(), order.unitPrice(),
                        order.subtotalAmount(), order.discountAmount(), order.payableAmount(), order.currency(),
                        order.priceVersion(), order.couponId(), order.inventoryReservationId(),
                        order.status().name())));
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
