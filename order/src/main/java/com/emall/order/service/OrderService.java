package com.emall.order.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.EventTypes;
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
import com.emall.order.workflow.OrderCreateWorkflow;
import com.emall.order.saga.OrderSagaCoordinator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ShardRoutingOperations shardRoutingOperations;
    private final ShardRouteIndex shardRouteIndex;
    private final OwnershipGuard ownershipGuard;
    private final BusinessMetrics businessMetrics;
    private final IdentityAccessGuard identityAccessGuard;
    private final RiskGuard riskGuard;
    private final IdempotencyService idempotencyService;
    private final OrderCreateWorkflow orderCreateWorkflow;
    private final OrderSubmissionGuard orderSubmissionGuard;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient) {
        this(orderRepository, outboxRepository, idGenerator, inventoryClient, pricingClient, marketingClient,
                ShardRoutingOperations.noop(), OwnershipGuard.noop(), BusinessMetrics.noop(),
                IdentityAccessGuard.noop(), RiskGuard.noop(), localIdempotencyService(), OrderSubmissionGuard.noop(),
                ShardRouteIndex.local(), OrderSagaCoordinator.local(orderRepository, inventoryClient, marketingClient));
    }

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, BusinessMetrics businessMetrics, IdentityAccessGuard identityAccessGuard,
            RiskGuard riskGuard, IdempotencyService idempotencyService, OrderSubmissionGuard orderSubmissionGuard) {
        this(orderRepository, outboxRepository, idGenerator, inventoryClient, pricingClient, marketingClient,
                shardRoutingOperations, ownershipGuard, businessMetrics, identityAccessGuard, riskGuard,
                idempotencyService, orderSubmissionGuard, ShardRouteIndex.local(),
                OrderSagaCoordinator.local(orderRepository, inventoryClient, marketingClient));
    }

    @Autowired
    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
            SnowflakeIdGenerator idGenerator, InventoryClient inventoryClient, PricingClient pricingClient,
            MarketingClient marketingClient, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, BusinessMetrics businessMetrics, IdentityAccessGuard identityAccessGuard,
            RiskGuard riskGuard, IdempotencyService idempotencyService, OrderSubmissionGuard orderSubmissionGuard,
            ShardRouteIndex shardRouteIndex, OrderSagaCoordinator sagaCoordinator) {
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
        this.orderCreateWorkflow = new OrderCreateWorkflow(orderRepository, outboxRepository, idGenerator,
                inventoryClient, pricingClient, marketingClient, businessMetrics, riskGuard, sagaCoordinator);
    }

    @Transactional
    public Order create(String requestId, long userId, long skuId, int quantity) {
        return create(requestId, userId, skuId, quantity, OrderClientContext.webDefault());
    }

    @Transactional
    public Order create(String requestId, long userId, long skuId, int quantity, OrderClientType clientType) {
        return create(requestId, userId, skuId, quantity, OrderClientContext.of(clientType,
                OrderClientContext.UNKNOWN_DEVICE, OrderClientContext.DIRECT_CHANNEL));
    }

    @Transactional
    public Order create(String requestId, long userId, long skuId, int quantity, OrderClientContext clientContext) {
        return create(requestId, userId, skuId, quantity, clientContext, null);
    }

    @Transactional
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
            return shardRoutingOperations.execute("order_record", routeUserId.getAsLong(),
                    () -> findByRequestId(requestId));
        }
        return findByRequestId(requestId);
    }

    private Order findByRequestId(String requestId) {
        return orderRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "idempotent order result is unavailable"));
    }

    public Order get(long orderId) {
        return shardRoutingOperations.execute("order_record", orderRouteKey(orderId), () -> orderRepository
                .findById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "order not found")));
    }

    public List<Order> findByStatus(OrderStatus status, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 1000));
        int shardCount = shardRoutingOperations.physicalShardCount("order_record");
        int perShardLimit = Math.max(1, (boundedLimit + shardCount - 1) / shardCount);
        return shardRoutingOperations
                .executeAll("order_record", () -> orderRepository.findByStatus(status, perShardLimit)).stream()
                .flatMap(List::stream).limit(boundedLimit).toList();
    }

    @Transactional
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
            return orderRepository.updateStatus(order.orderId(), OrderStatus.CREATED, pending)
                    ? pending
                    : get(order.orderId());
        }
        if (!marketingClient.confirmCoupon(order.requestId(), order.couponId(), order.orderId())) {
            Order pending = order.markPendingRetry("coupon confirm pending");
            return orderRepository.updateStatus(order.orderId(), OrderStatus.CREATED, pending)
                    ? pending
                    : get(order.orderId());
        }
        Order paid = order.markPaid();
        if (!orderRepository.updateStatus(order.orderId(), OrderStatus.CREATED, paid)) {
            return get(order.orderId());
        }
        appendEvent(paid, EventTypes.ORDER_PAID);
        businessMetrics.increment(BusinessMetricNames.ORDER_PAID, "channel", order.channel());
        return paid;
    }

    @Transactional
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
            return orderRepository.updateStatus(order.orderId(), order.status(), pending)
                    ? pending
                    : get(order.orderId());
        }
        if (!marketingClient.releaseCoupon(order.requestId(), order.couponId(), order.orderId())) {
            Order pending = order.markPendingRetry("coupon release pending");
            return orderRepository.updateStatus(order.orderId(), order.status(), pending)
                    ? pending
                    : get(order.orderId());
        }
        Order cancelled = order.markCancelled();
        if (!orderRepository.updateStatus(order.orderId(), order.status(), cancelled)) {
            return get(order.orderId());
        }
        appendEvent(cancelled, EventTypes.ORDER_CANCELLED);
        businessMetrics.increment(BusinessMetricNames.ORDER_CANCELLED, "channel", order.channel());
        return cancelled;
    }

    @Transactional
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
            if (!orderRepository.updateStatus(order.orderId(), OrderStatus.PENDING_RETRY, created)) {
                return get(order.orderId());
            }
            appendEvent(created, EventTypes.ORDER_CREATED);
            return created;
        }
        Order pending = order.markPendingRetry(reservation.reason());
        return orderRepository.updateStatus(order.orderId(), OrderStatus.PENDING_RETRY, pending)
                ? pending
                : get(order.orderId());
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
            if (!orderRepository.updateStatus(order.orderId(), OrderStatus.PENDING_RETRY, paid)) {
                return get(order.orderId());
            }
            appendEvent(paid, EventTypes.ORDER_PAID);
            businessMetrics.increment(BusinessMetricNames.ORDER_PAID, "channel", order.channel());
            return paid;
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
            if (!orderRepository.updateStatus(order.orderId(), OrderStatus.PENDING_RETRY, cancelled)) {
                return get(order.orderId());
            }
            appendEvent(cancelled, EventTypes.ORDER_CANCELLED);
            businessMetrics.increment(BusinessMetricNames.ORDER_CANCELLED, "channel", order.channel());
            return cancelled;
        }
        return order;
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

    private long orderRouteKey(long orderId) {
        return shardRouteIndex.resolveRequired("order-id", Long.toString(orderId), orderId);
    }

    private static IdempotencyService localIdempotencyService() {
        return new IdempotencyService(new InMemoryIdempotencyRepository(), Clock.systemUTC(), Duration.ofSeconds(30),
                Duration.ofDays(1));
    }
}
