package com.emall.payment.service;

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
import com.emall.common.trust.ClientTrustContext;
import com.emall.common.trust.IdentityAccessGuard;
import com.emall.common.trust.RiskEvaluationRequest;
import com.emall.common.trust.RiskGuard;
import com.emall.common.trust.RiskScene;
import com.emall.payment.domain.LedgerDirection;
import com.emall.payment.domain.PaymentChannelStatement;
import com.emall.payment.domain.PaymentLedgerEntry;
import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.domain.PaymentReconciliationRecord;
import com.emall.payment.domain.PaymentRefundOrder;
import com.emall.payment.domain.PaymentRefundStatus;
import com.emall.payment.domain.PaymentStatus;
import com.emall.payment.domain.ReconciliationStatus;
import com.emall.payment.domain.StatementType;
import com.emall.payment.integration.OrderClient;
import com.emall.payment.repository.PaymentRepository;
import com.emall.payment.repository.PaymentSettlementRepository;
import com.emall.payment.security.PaymentCallbackVerifier;
import com.emall.payment.security.PaymentSecurityProperties;
import java.math.BigDecimal;
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
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentSettlementRepository settlementRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final OrderClient orderClient;
    private final PaymentCallbackVerifier callbackVerifier;
    private final ShardRoutingOperations shardRoutingOperations;
    private final OwnershipGuard ownershipGuard;
    private final BusinessMetrics businessMetrics;
    private final IdentityAccessGuard identityAccessGuard;
    private final RiskGuard riskGuard;
    private final IdempotencyService idempotencyService;

    public PaymentService(PaymentRepository paymentRepository, PaymentSettlementRepository settlementRepository,
            OutboxRepository outboxRepository, SnowflakeIdGenerator idGenerator, OrderClient orderClient) {
        this(paymentRepository, settlementRepository, outboxRepository, idGenerator, orderClient,
                new PaymentCallbackVerifier(new PaymentSecurityProperties(), Clock.systemUTC()),
                ShardRoutingOperations.noop(), OwnershipGuard.noop(), BusinessMetrics.noop(),
                IdentityAccessGuard.noop(), RiskGuard.noop(), localIdempotencyService());
    }

    public PaymentService(PaymentRepository paymentRepository, PaymentSettlementRepository settlementRepository,
            OutboxRepository outboxRepository, SnowflakeIdGenerator idGenerator, OrderClient orderClient,
            PaymentCallbackVerifier callbackVerifier) {
        this(paymentRepository, settlementRepository, outboxRepository, idGenerator, orderClient, callbackVerifier,
                ShardRoutingOperations.noop(), OwnershipGuard.noop(), BusinessMetrics.noop(),
                IdentityAccessGuard.noop(), RiskGuard.noop(), localIdempotencyService());
    }

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, PaymentSettlementRepository settlementRepository,
            OutboxRepository outboxRepository, SnowflakeIdGenerator idGenerator, OrderClient orderClient,
            PaymentCallbackVerifier callbackVerifier, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, BusinessMetrics businessMetrics, IdentityAccessGuard identityAccessGuard,
            RiskGuard riskGuard, IdempotencyService idempotencyService) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.orderClient = orderClient;
        this.callbackVerifier = callbackVerifier;
        this.shardRoutingOperations = shardRoutingOperations;
        this.ownershipGuard = ownershipGuard;
        this.businessMetrics = businessMetrics;
        this.identityAccessGuard = identityAccessGuard;
        this.riskGuard = riskGuard;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PaymentOrder create(String requestId, long orderId, long userId, BigDecimal amount, String channel) {
        return create(requestId, orderId, userId, amount, channel, null);
    }

    @Transactional
    public PaymentOrder create(String requestId, long orderId, long userId, BigDecimal amount, String channel,
            ClientTrustContext trustContext) {
        ClientTrustContext safeTrustContext = normalizeTrustContext(trustContext, userId, channel);
        identityAccessGuard.requireAccess(safeTrustContext, userId, "payment:create", "user:" + userId);
        riskGuard.check(new RiskEvaluationRequest(RiskScene.PAYMENT, safeTrustContext.subjectId(userId),
                safeTrustContext.deviceId(), safeTrustContext.sourceIp(), amount, 1));
        IdempotencyKey key = IdempotencyKey.of("payment", String.valueOf(userId), requestId, "create");
        String requestDigest = idempotencyService
                .digest("orderId=" + orderId + ",userId=" + userId + ",amount=" + amount + ",channel=" + channel);
        return IdempotencyExecutor.execute(idempotencyService, key, "Payment", String.valueOf(orderId), requestDigest,
                () -> createIdempotent(requestId, orderId, userId, amount, channel), ignored -> replayCreate(requestId),
                payment -> idempotencyService
                        .digest("paymentId=" + payment.paymentId() + ",status=" + payment.status()));
    }

    private PaymentOrder createIdempotent(String requestId, long orderId, long userId, BigDecimal amount,
            String channel) {
        long routeOrderId = paymentRepository.findRouteOrderIdByRequestId(requestId).orElse(orderId);
        return shardRoutingOperations.execute("payment_order", routeOrderId, () -> {
            ownershipGuard.checkWrite("payment", orderId);
            return paymentRepository.findByRequestId(requestId)
                    .map(existing -> validateIdempotentCreate(existing, orderId, userId, amount, channel))
                    .orElseGet(() -> createOnce(requestId, orderId, userId, amount, channel));
        });
    }

    private PaymentOrder replayCreate(String requestId) {
        Optional<Long> routeOrderId = paymentRepository.findRouteOrderIdByRequestId(requestId);
        if (routeOrderId.isPresent()) {
            return shardRoutingOperations.execute("payment_order", routeOrderId.get(),
                    () -> findByRequestId(requestId));
        }
        return findByRequestId(requestId);
    }

    private PaymentOrder findByRequestId(String requestId) {
        return paymentRepository.findByRequestId(requestId).orElseThrow(
                () -> new BusinessException(ErrorCode.CONFLICT, "idempotent payment result is unavailable"));
    }

    public PaymentOrder get(long paymentId) {
        return shardRoutingOperations.execute("payment_order", paymentRouteKey(paymentId),
                () -> paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "payment not found")));
    }

    @Transactional
    public PaymentOrder callback(String channelTradeNo, long paymentId, BigDecimal paidAmount) {
        return callback(null, channelTradeNo, paymentId, paidAmount);
    }

    @Transactional
    public PaymentOrder callback(PaymentCallbackCommand command) {
        callbackVerifier.verify(command);
        return callback(command.channel(), command.channelTradeNo(), command.paymentId(), command.paidAmount());
    }

    private PaymentOrder callback(String channel, String channelTradeNo, long paymentId, BigDecimal paidAmount) {
        String ownerId = channel == null ? "legacy" : channel;
        IdempotencyKey key = IdempotencyKey.of("payment-callback", ownerId, channelTradeNo, "callback");
        String requestDigest = idempotencyService.digest("channel=" + channel + ",tradeNo=" + channelTradeNo
                + ",paymentId=" + paymentId + ",paidAmount=" + paidAmount);
        return IdempotencyExecutor.execute(idempotencyService, key, "PaymentCallback", String.valueOf(paymentId),
                requestDigest,
                () -> shardRoutingOperations.execute("payment_order", paymentRouteKey(paymentId),
                        () -> callbackInShard(channel, channelTradeNo, paymentId, paidAmount)),
                ignored -> get(paymentId), payment -> idempotencyService
                        .digest("paymentId=" + payment.paymentId() + ",status=" + payment.status()));
    }

    private PaymentOrder callbackInShard(String channel, String channelTradeNo, long paymentId, BigDecimal paidAmount) {
        ownershipGuard.checkWrite("payment", paymentId);
        paymentRepository.findByChannelAndTradeNo(channel, channelTradeNo).ifPresent(existing -> {
            if (existing.paymentId() != paymentId) {
                throw new BusinessException(ErrorCode.CONFLICT, "channel trade no already used");
            }
        });
        PaymentOrder payment = get(paymentId);
        if (payment.status() == PaymentStatus.SUCCEEDED) {
            return payment;
        }
        if (channel != null && !payment.channel().equals(channel)) {
            throw new BusinessException(ErrorCode.CONFLICT, "payment channel mismatch");
        }
        if (payment.amount().compareTo(paidAmount) != 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "paid amount mismatch");
        }
        PaymentOrder succeeded = payment.succeed(channelTradeNo);
        if (!paymentRepository.updateStatus(payment.paymentId(), PaymentStatus.CREATED, succeeded)) {
            return get(payment.paymentId());
        }
        appendDoubleEntry(succeeded, "PAYMENT", "payment:" + succeeded.paymentId(), "CHANNEL_CASH", "USER_PAYABLE");
        appendEvent(succeeded, EventTypes.PAYMENT_SUCCEEDED);
        businessMetrics.increment(BusinessMetricNames.PAYMENT_SUCCEEDED, "channel", succeeded.channel());
        boolean orderUpdated = orderClient.payOrder(payment.orderId());
        if (!orderUpdated) {
            businessMetrics.increment(BusinessMetricNames.PAYMENT_ORDER_CONFIRM_FAILED, "channel", succeeded.channel());
            return succeeded;
        }
        PaymentOrder confirmed = succeeded.confirmOrder();
        return paymentRepository.markOrderConfirmed(payment.paymentId(), PaymentStatus.SUCCEEDED, confirmed)
                ? confirmed
                : get(payment.paymentId());
    }

    @Transactional
    public PaymentOrder refund(long paymentId) {
        return refund(paymentId, null);
    }

    @Transactional
    public PaymentOrder refund(long paymentId, ClientTrustContext trustContext) {
        IdempotencyKey key = IdempotencyKey.of("payment", String.valueOf(paymentId), "refund-" + paymentId, "refund");
        String requestDigest = idempotencyService.digest("paymentId=" + paymentId);
        return IdempotencyExecutor.execute(idempotencyService, key, "PaymentRefund", String.valueOf(paymentId),
                requestDigest,
                () -> shardRoutingOperations.execute("payment_order", paymentRouteKey(paymentId),
                        () -> refundInShard(paymentId, trustContext)),
                ignored -> get(paymentId), payment -> idempotencyService
                        .digest("paymentId=" + payment.paymentId() + ",status=" + payment.status()));
    }

    private PaymentOrder refundInShard(long paymentId, ClientTrustContext trustContext) {
        ownershipGuard.checkWrite("payment", paymentId);
        PaymentOrder payment = get(paymentId);
        ClientTrustContext safeTrustContext = normalizeTrustContext(trustContext, payment.userId(), payment.channel());
        identityAccessGuard.requireAccess(safeTrustContext, payment.userId(), "payment:refund",
                "user:" + payment.userId());
        riskGuard.check(new RiskEvaluationRequest(RiskScene.REFUND, safeTrustContext.subjectId(payment.userId()),
                safeTrustContext.deviceId(), safeTrustContext.sourceIp(), payment.amount(), 1));
        if (payment.status() == PaymentStatus.REFUNDED) {
            return payment;
        }
        if (payment.status() != PaymentStatus.SUCCEEDED && payment.status() != PaymentStatus.REFUNDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "payment cannot be refunded from " + payment.status());
        }
        PaymentRefundOrder refundOrder =
                settlementRepository.saveRefundIfAbsent(new PaymentRefundOrder(idGenerator.nextId(),
                        payment.paymentId(), "refund-" + payment.paymentId(), payment.channel(), null, payment.amount(),
                        PaymentRefundStatus.CREATED, "full refund", Instant.now(), Instant.now()));
        PaymentRefundOrder processing = refundOrder.processing();
        if (!settlementRepository.updateRefundStatus(refundOrder.refundId(), PaymentRefundStatus.CREATED, processing)
                && refundOrder.status() != PaymentRefundStatus.PROCESSING
                && refundOrder.status() != PaymentRefundStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.CONFLICT, "refund order status changed during refund");
        }
        PaymentRefundOrder succeededRefund = processing.succeeded("local-refund-" + payment.paymentId());
        settlementRepository.updateRefundStatus(processing.refundId(), PaymentRefundStatus.PROCESSING, succeededRefund);
        PaymentOrder refunded = payment.refunding().refunded();
        if (!paymentRepository.updateStatus(payment.paymentId(), payment.status(), refunded)) {
            return get(payment.paymentId());
        }
        appendDoubleEntry(refunded, "REFUND", "refund:" + succeededRefund.refundId(), "USER_PAYABLE", "CHANNEL_CASH");
        appendEvent(refunded, EventTypes.PAYMENT_REFUNDED);
        businessMetrics.increment(BusinessMetricNames.PAYMENT_REFUNDED, "channel", refunded.channel());
        return refunded;
    }

    @Transactional
    public PaymentChannelStatement ingestChannelStatement(String channel, String channelTradeNo, long paymentId,
            BigDecimal amount, StatementType statementType, Instant occurredAt) {
        if (amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "statement amount must be positive");
        }
        Instant now = Instant.now();
        return settlementRepository.saveStatementIfAbsent(new PaymentChannelStatement(idGenerator.nextId(), channel,
                channelTradeNo, paymentId, amount, statementType, occurredAt, false, now));
    }

    public List<PaymentChannelStatement> findUnreconciledStatements(int limit) {
        return settlementRepository.findUnreconciledStatements(limit);
    }

    @Transactional
    public PaymentReconciliationRecord reconcileStatement(long statementId) {
        PaymentChannelStatement statement = settlementRepository.findUnreconciledStatementById(statementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "statement not found"));
        ownershipGuard.checkWrite("payment", statement.paymentId());
        PaymentReconciliationRecord record = buildReconciliationRecord(statement);
        settlementRepository.saveReconciliationIfAbsent(record);
        settlementRepository.markStatementReconciled(statement.statementId());
        return record;
    }

    @Transactional
    public int reconcileChannelStatements(int limit) {
        return settlementRepository.findUnreconciledStatements(limit).stream()
                .map(statement -> reconcileStatement(statement.statementId())).toList().size();
    }

    public List<PaymentOrder> findSucceededButUnconfirmed(int limit) {
        return paymentRepository.findUnconfirmedByStatus(PaymentStatus.SUCCEEDED, limit);
    }

    @Transactional
    public PaymentOrder retryOrderConfirmation(long paymentId) {
        return shardRoutingOperations.execute("payment_order", paymentRouteKey(paymentId),
                () -> retryOrderConfirmationInShard(paymentId));
    }

    private PaymentOrder retryOrderConfirmationInShard(long paymentId) {
        ownershipGuard.checkWrite("payment", paymentId);
        PaymentOrder payment = get(paymentId);
        if (payment.status() != PaymentStatus.SUCCEEDED || payment.orderConfirmed()) {
            return payment;
        }
        if (orderClient.payOrder(payment.orderId())) {
            PaymentOrder confirmed = payment.confirmOrder();
            return paymentRepository.markOrderConfirmed(payment.paymentId(), PaymentStatus.SUCCEEDED, confirmed)
                    ? confirmed
                    : get(payment.paymentId());
        }
        return payment;
    }

    private PaymentOrder createOnce(String requestId, long orderId, long userId, BigDecimal amount, String channel) {
        if (amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "amount must be positive");
        }
        Instant now = Instant.now();
        PaymentOrder payment = paymentRepository.save(new PaymentOrder(idGenerator.nextId(), requestId, orderId, userId,
                amount, channel, null, PaymentStatus.CREATED, false, now, now));
        paymentRepository.saveRoute(payment.paymentId(), payment.requestId(), payment.orderId(), payment.userId());
        return payment;
    }

    private PaymentOrder validateIdempotentCreate(PaymentOrder existing, long orderId, long userId, BigDecimal amount,
            String channel) {
        if (existing.orderId() != orderId || existing.userId() != userId || existing.amount().compareTo(amount) != 0
                || !existing.channel().equals(channel)) {
            throw new BusinessException(ErrorCode.CONFLICT, "requestId already used by different payment request");
        }
        return existing;
    }

    private ClientTrustContext normalizeTrustContext(ClientTrustContext trustContext, long userId, String channel) {
        ClientTrustContext base = trustContext == null ? ClientTrustContext.anonymous() : trustContext;
        return base.withDefaults(userId, null, channel);
    }

    private void appendDoubleEntry(PaymentOrder payment, String businessType, String referenceId, String debitAccount,
            String creditAccount) {
        appendLedger(payment, LedgerDirection.DEBIT, debitAccount, businessType, referenceId);
        appendLedger(payment, LedgerDirection.CREDIT, creditAccount, businessType, referenceId);
    }

    private void appendLedger(PaymentOrder payment, LedgerDirection direction, String accountCode, String businessType,
            String referenceId) {
        settlementRepository.saveLedgerIfAbsent(
                new PaymentLedgerEntry(idGenerator.nextId(), payment.paymentId(), payment.orderId(), payment.userId(),
                        direction, accountCode, payment.amount(), "CNY", businessType, referenceId, Instant.now()));
    }

    private PaymentReconciliationRecord buildReconciliationRecord(PaymentChannelStatement statement) {
        PaymentOrder payment = paymentRepository.findById(statement.paymentId()).orElse(null);
        if (payment == null) {
            return reconciliation(statement, ReconciliationStatus.PAYMENT_NOT_FOUND, "payment not found");
        }
        if (!statement.channelTradeNo().equals(payment.channelTradeNo())) {
            return reconciliation(statement, ReconciliationStatus.TRADE_NO_MISMATCH, "channel trade no mismatch");
        }
        if (statement.amount().compareTo(payment.amount()) != 0) {
            return reconciliation(statement, ReconciliationStatus.AMOUNT_MISMATCH, "amount mismatch");
        }
        if (!statusMatches(statement.statementType(), payment.status())) {
            return reconciliation(statement, ReconciliationStatus.STATUS_MISMATCH, "payment status mismatch");
        }
        return reconciliation(statement, ReconciliationStatus.MATCHED, "matched");
    }

    private boolean statusMatches(StatementType statementType, PaymentStatus paymentStatus) {
        if (statementType == StatementType.PAYMENT) {
            return paymentStatus == PaymentStatus.SUCCEEDED || paymentStatus == PaymentStatus.REFUNDED;
        }
        return paymentStatus == PaymentStatus.REFUNDED;
    }

    private PaymentReconciliationRecord reconciliation(PaymentChannelStatement statement, ReconciliationStatus status,
            String message) {
        return new PaymentReconciliationRecord(idGenerator.nextId(), statement.statementId(), statement.paymentId(),
                statement.channelTradeNo(), statement.statementType(), status, message, Instant.now());
    }

    private void appendEvent(PaymentOrder payment, String eventType) {
        outboxRepository.save(OutboxEvent.create("payment-event-" + idGenerator.nextId(), "Payment",
                String.valueOf(payment.paymentId()), eventType,
                Map.of("paymentId", payment.paymentId(), "orderId", payment.orderId(), "userId", payment.userId(),
                        "amount", payment.amount(), "status", payment.status().name())));
    }

    private long paymentRouteKey(long paymentId) {
        return paymentRepository.findRouteOrderIdByPaymentId(paymentId).orElse(paymentId);
    }

    private static IdempotencyService localIdempotencyService() {
        return new IdempotencyService(new InMemoryIdempotencyRepository(), Clock.systemUTC(), Duration.ofSeconds(30),
                Duration.ofDays(1));
    }
}
