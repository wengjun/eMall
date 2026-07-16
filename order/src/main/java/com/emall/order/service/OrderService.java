package com.emall.order.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OrderEventPayload;
import com.emall.common.event.OutboxEvent;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.idempotency.IdempotencyExecutor;
import com.emall.common.idempotency.IdempotencyKey;
import com.emall.common.idempotency.IdempotencyService;
import com.emall.common.idempotency.InMemoryIdempotencyRepository;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.outbox.OutboxRepository;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.common.sharding.ShardRouteIndex;
import com.emall.common.trust.ClientTrustContext;
import com.emall.common.trust.IdentityAccessGuard;
import com.emall.common.trust.RiskEvaluationRequest;
import com.emall.common.trust.RiskGuard;
import com.emall.common.trust.RiskScene;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientContext;
import com.emall.order.domain.OrderClientType;
import com.emall.order.domain.OrderStatus;
import com.emall.order.integration.InventoryClient;
import com.emall.order.integration.InventoryClient.InventoryReservation;
import com.emall.order.integration.InventoryClient.ReserveInventoryRequest;
import com.emall.order.integration.MarketingClient;
import com.emall.order.integration.PricingClient;
import com.emall.order.repository.OrderRepository;
import com.emall.order.saga.OrderSagaCoordinator;
import com.emall.order.transaction.OrderLocalTransaction;
import com.emall.order.workflow.OrderCreateWorkflow;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final InventoryClient inventoryClient;
    private final PricingClient pricingClient;
    private final MarketingClient marketingClient;
    private final ShardRoutingOperations shardRoutingOperations;
    private final ShardRouteIndex shardRouteIndex;
    private final OwnershipGuard ownershipGuard;
    private final BusinessMetrics businessMetrics;
    private final IdentityAccessGuard identityAccessGuard;
    private final RiskGuard riskGuard;
    private final IdempotencyService idempotencyService;
    private final OrderCreateWorkflow orderCreateWorkflow;
    private final OrderSubmissionGuard orderSubmissionGuard;
    private final OrderLocalTransaction localTransaction;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient) {
        this(orderRepository, outboxRepository, idGenerator, inventoryClient, pricingClient, marketingClient,
                ShardRoutingOperations.noop(), OwnershipGuard.noop(), BusinessMetrics.noop(),
                IdentityAccessGuard.noop(), RiskGuard.noop(), localIdempotencyService(), OrderSubmissionGuard.noop(),
                ShardRouteIndex.local(), OrderSagaCoordinator.local(orderRepository, inventoryClient, marketingClient),
                OrderLocalTransaction.direct());
    }

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, BusinessMetrics businessMetrics, IdentityAccessGuard identityAccessGuard,
            RiskGuard riskGuard, IdempotencyService idempotencyService, OrderSubmissionGuard orderSubmissionGuard) {
        this(orderRepository, outboxRepository, idGenerator, inventoryClient, pricingClient, marketingClient,
                shardRoutingOperations, ownershipGuard, businessMetrics, identityAccessGuard, riskGuard,
                idempotencyService, orderSubmissionGuard, ShardRouteIndex.local(),
                OrderSagaCoordinator.local(orderRepository, inventoryClient, marketingClient),
                OrderLocalTransaction.direct());
    }

    @Autowired
    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, BusinessMetrics businessMetrics, IdentityAccessGuard identityAccessGuard,
            RiskGuard riskGuard, IdempotencyService idempotencyService, OrderSubmissionGuard orderSubmissionGuard,
            ShardRouteIndex shardRouteIndex, OrderSagaCoordinator sagaCoordinator,
            OrderLocalTransaction localTransaction) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.inventoryClient = inventoryClient;
        this.pricingClient = pricingClient;
        this.marketingClient = marketingClient;
        this.shardRoutingOperations = shardRoutingOperations;
        this.shardRouteIndex = shardRouteIndex;
        this.ownershipGuard = ownershipGuard;
        this.businessMetrics = businessMetrics;
        this.identityAccessGuard = identityAccessGuard;
        this.riskGuard = riskGuard;
        this.idempotencyService = idempotencyService;
        this.orderSubmissionGuard = orderSubmissionGuard;
        this.localTransaction = localTransaction;
        this.orderCreateWorkflow =
                new OrderCreateWorkflow(orderRepository, outboxRepository, idGenerator, inventoryClient, pricingClient,
                        marketingClient, businessMetrics, riskGuard, sagaCoordinator, localTransaction);
    }

    public Order create(String requestId, long userId, long skuId, int quantity) {
        return create(requestId, userId, skuId, quantity, OrderClientContext.webDefault());
    }

    public Order create(String requestId, long userId, long skuId, int quantity, OrderClientType clientType) {
        return create(requestId, userId, skuId, quantity, OrderClientContext.of(clientType,
                OrderClientContext.UNKNOWN_DEVICE, OrderClientContext.DIRECT_CHANNEL));
    }

    public Order create(String requestId, long userId, long skuId, int quantity, OrderClientContext clientContext) {
        return create(requestId, userId, skuId, quantity, clientContext, null);
    }

    public Order create(String requestId, long userId, long skuId, int quantity, OrderClientContext clientContext,
            ClientTrustContext trustContext) {
        OrderClientContext safeContext = clientContext == null ? OrderClientContext.webDefault() : clientContext;
        ClientTrustContext safeTrustContext = normalizeTrustContext(trustContext, userId, safeContext);
        identityAccessGuard.requireAccess(safeTrustContext, userId, "order:create", "user:" + userId);
        orderSubmissionGuard.check(userId);
        IdempotencyKey key = IdempotencyKey.of("order", String.valueOf(userId), requestId, "create");
        String requestDigest = idempotencyService.digest("userId=" + userId + ",skuId=" + skuId + ",quantity="
                + quantity + ",clientType=" + safeContext.clientType() + ",deviceId=" + safeContext.deviceId()
                + ",channel=" + safeContext.channel());
        return shardRoutingOperations.execute("order_record", userId, () -> IdempotencyExecutor.execute(
                idempotencyService, key, "Order", String.valueOf(userId), requestDigest,
                () -> createIdempotent(requestId, userId, skuId, quantity, safeContext, safeTrustContext),
                ignored -> replayCreate(requestId),
                order -> idempotencyService.digest("orderId=" + order.orderId() + ",status=" + order.status())));
    }

    private Order createIdempotent(String requestId, long userId, long skuId, int quantity,
            OrderClientContext safeContext, ClientTrustContext safeTrustContext) {
        long routeUserId = shardRouteIndex.resolve("order-request", requestId).orElse(userId);
        return shardRoutingOperations.execute("order_record", routeUserId, () -> {
            ownershipGuard.checkWrite("order", userId);
            Order order = orderRepository.findByRequestId(requestId)
                    .map(existing -> validateIdempotentCreate(existing, userId, skuId, quantity, safeContext))
                    .orElseGet(() -> orderCreateWorkflow.create(requestId, userId, skuId, quantity, safeContext,
                            safeTrustContext));
            shardRouteIndex.bindUniqueTransactional("order-id", Long.toString(order.orderId()), order.userId());
            shardRouteIndex.bindUniqueTransactional("order-request", order.requestId(), order.userId());
            return order;
        });
    }

    private Order replayCreate(String requestId) {
        var routeUserId = shardRouteIndex.resolve("order-request", requestId);
        if (routeUserId.isPresent()) {
            return shardRoutingOperations.executeRead("order_record", routeUserId.getAsLong(),
                    () -> findByRequestId(requestId));
        }
        return findByRequestId(requestId);
    }

    private Order findByRequestId(String requestId) {
        return orderRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "idempotent order result is unavailable"));
    }

    public Order get(long orderId) {
        return shardRoutingOperations.executeRead("order_record", orderRouteKey(orderId), () -> orderRepository
                .findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order not found")));
    }

    public List<Order> findByStatus(OrderStatus status, int limit) {
        return orderRepository.findByStatus(status, Math.max(1, Math.min(limit, 1000)));
    }

    public Order pay(long orderId) {
        return shardRoutingOperations.execute("order_record", orderRouteKey(orderId), () -> payInShard(orderId));
    }

    private Order payInShard(long orderId) {
        ownershipGuard.checkWrite("order", orderId);
        Order order = get(orderId);
        if (order.status() == OrderStatus.PAID) {
            return order;
        }
        if (order.status() != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.CONFLICT, "order cannot be paid from " + order.status());
        }
        InventoryReservation reservation = inventoryClient.confirm(order.inventoryReservationId());
        if (reservation == null || !reservation.confirmed()) {
            Order pending = order.markPendingRetry("inventory confirm pending");
            return persistTransition("pay-pending-inventory", order, OrderStatus.CREATED, pending, null);
        }
        if (!marketingClient.confirmCoupon(order.requestId(), order.couponId(), order.orderId())) {
            Order pending = order.markPendingRetry("coupon confirm pending");
            return persistTransition("pay-pending-coupon", order, OrderStatus.CREATED, pending, null);
        }
        Order paid = order.markPaid();
        Order persisted = persistTransition("pay", order, OrderStatus.CREATED, paid, EventTypes.ORDER_PAID);
        if (persisted.equals(paid)) {
            businessMetrics.increment(BusinessMetricNames.ORDER_PAID, "channel", order.channel());
        }
        return persisted;
    }

    public Order cancel(long orderId) {
        return shardRoutingOperations.execute("order_record", orderRouteKey(orderId), () -> cancelInShard(orderId));
    }

    private Order cancelInShard(long orderId) {
        ownershipGuard.checkWrite("order", orderId);
        Order order = get(orderId);
        if (order.status() == OrderStatus.CANCELLED || order.status() == OrderStatus.CLOSED) {
            return order;
        }
        if (order.status() == OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.CONFLICT, "paid order requires refund flow");
        }
        InventoryReservation reservation = inventoryClient.release(order.inventoryReservationId());
        if (reservation == null || !reservation.released()) {
            Order pending = order.markPendingRetry("inventory release pending");
            return persistTransition("cancel-pending-inventory", order, order.status(), pending, null);
        }
        if (!marketingClient.releaseCoupon(order.requestId(), order.couponId(), order.orderId())) {
            Order pending = order.markPendingRetry("coupon release pending");
            return persistTransition("cancel-pending-coupon", order, order.status(), pending, null);
        }
        Order cancelled = order.markCancelled();
        Order persisted = persistTransition("cancel", order, order.status(), cancelled, EventTypes.ORDER_CANCELLED);
        if (persisted.equals(cancelled)) {
            businessMetrics.increment(BusinessMetricNames.ORDER_CANCELLED, "channel", order.channel());
        }
        return persisted;
    }

    public Order retryPending(long orderId) {
        return shardRoutingOperations.execute("order_record", orderRouteKey(orderId),
                () -> retryPendingInShard(orderId));
    }

    private Order retryPendingInShard(long orderId) {
        ownershipGuard.checkWrite("order", orderId);
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
            Order created = order.markCreated();
            return persistTransition("retry-reserve", order, OrderStatus.PENDING_RETRY, created,
                    EventTypes.ORDER_CREATED);
        }
        Order pending = order.markPendingRetry(reservation.reason());
        return persistTransition("retry-pending", order, OrderStatus.PENDING_RETRY, pending, null);
    }

    private Order validateIdempotentCreate(Order existing, long userId, long skuId, int quantity,
            OrderClientContext clientContext) {
        if (existing.userId() != userId || existing.skuId() != skuId || existing.quantity() != quantity
                || existing.clientType() != clientContext.clientType()
                || !existing.deviceId().equals(clientContext.deviceId())
                || !existing.channel().equals(clientContext.channel())) {
            throw new BusinessException(ErrorCode.CONFLICT, "requestId already used by different order request");
        }
        return existing;
    }

    private ClientTrustContext normalizeTrustContext(ClientTrustContext trustContext, long userId,
            OrderClientContext clientContext) {
        ClientTrustContext base = trustContext == null ? ClientTrustContext.anonymous() : trustContext;
        return base.withDefaults(userId, clientContext.deviceId(), clientContext.channel());
    }

    private Order payAfterRetry(Order order) {
        InventoryReservation reservation = inventoryClient.confirm(order.inventoryReservationId());
        if (reservation != null && reservation.confirmed()) {
            if (!marketingClient.confirmCoupon(order.requestId(), order.couponId(), order.orderId())) {
                return order;
            }
            Order paid = order.markPaid();
            Order persisted =
                    persistTransition("retry-pay", order, OrderStatus.PENDING_RETRY, paid, EventTypes.ORDER_PAID);
            if (persisted.equals(paid)) {
                businessMetrics.increment(BusinessMetricNames.ORDER_PAID, "channel", order.channel());
            }
            return persisted;
        }
        return order;
    }

    private Order cancelAfterRetry(Order order) {
        InventoryReservation reservation = inventoryClient.release(order.inventoryReservationId());
        if (reservation != null && reservation.released()) {
            if (!marketingClient.releaseCoupon(order.requestId(), order.couponId(), order.orderId())) {
                return order;
            }
            Order cancelled = order.markCancelled();
            Order persisted = persistTransition("retry-cancel", order, OrderStatus.PENDING_RETRY, cancelled,
                    EventTypes.ORDER_CANCELLED);
            if (persisted.equals(cancelled)) {
                businessMetrics.increment(BusinessMetricNames.ORDER_CANCELLED, "channel", order.channel());
            }
            return persisted;
        }
        return order;
    }

    private Order persistTransition(String operation, Order current, OrderStatus expectedStatus, Order next,
            String eventType) {
        return localTransaction.execute(operation, () -> {
            if (!orderRepository.updateStatus(current.orderId(), expectedStatus, next)) {
                return get(current.orderId());
            }
            if (eventType != null) {
                appendEvent(next, eventType);
            }
            return next;
        });
    }

    private void appendEvent(Order order, String eventType) {
        outboxRepository.save(OutboxEvent.create("order-event-" + idGenerator.nextId(), "Order",
                String.valueOf(order.orderId()), eventType, "order", "0.1.0", orderEventPayload(order)));
    }

    private OrderEventPayload orderEventPayload(Order order) {
        return new OrderEventPayload(order.orderId(), order.userId(), order.skuId(), order.quantity(),
                order.clientType().name(), order.deviceId(), order.channel(), order.unitPrice(), order.subtotalAmount(),
                order.discountAmount(), order.payableAmount(), order.currency(), order.priceVersion(), order.couponId(),
                order.inventoryReservationId(), order.status().name());
    }

    private long orderRouteKey(long orderId) {
        return shardRouteIndex.resolveRequired("order-id", Long.toString(orderId), orderId);
    }

    private static IdempotencyService localIdempotencyService() {
        return new IdempotencyService(new InMemoryIdempotencyRepository(), Clock.systemUTC(), Duration.ofSeconds(30),
                Duration.ofDays(1));
    }
}
