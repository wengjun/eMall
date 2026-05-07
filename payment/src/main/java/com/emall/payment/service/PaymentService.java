package com.emall.payment.service;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.outbox.OutboxRepository;
import com.emall.payment.domain.LedgerDirection;
import com.emall.payment.domain.PaymentChannelStatement;
import com.emall.payment.domain.PaymentLedgerEntry;
import com.emall.payment.domain.PaymentOrder;
import com.emall.payment.domain.PaymentReconciliationRecord;
import com.emall.payment.domain.PaymentStatus;
import com.emall.payment.domain.ReconciliationStatus;
import com.emall.payment.domain.StatementType;
import com.emall.payment.integration.OrderClient;
import com.emall.payment.repository.PaymentRepository;
import com.emall.payment.repository.PaymentSettlementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentSettlementRepository settlementRepository;
    private final OutboxRepository outboxRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final OrderClient orderClient;

    public PaymentService(PaymentRepository paymentRepository, PaymentSettlementRepository settlementRepository,
            OutboxRepository outboxRepository, SnowflakeIdGenerator idGenerator, OrderClient orderClient) {
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
        this.outboxRepository = outboxRepository;
        this.idGenerator = idGenerator;
        this.orderClient = orderClient;
    }

    @Transactional
    public synchronized PaymentOrder create(String requestId, long orderId, long userId, BigDecimal amount,
            String channel) {
        return paymentRepository.findByRequestId(requestId)
                .orElseGet(() -> createOnce(requestId, orderId, userId, amount, channel));
    }

    public PaymentOrder get(long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "payment not found"));
    }

    @Transactional
    public synchronized PaymentOrder callback(String channelTradeNo, long paymentId, BigDecimal paidAmount) {
        paymentRepository.findByChannelTradeNo(channelTradeNo).ifPresent(existing -> {
            if (existing.paymentId() != paymentId) {
                throw new BusinessException(ErrorCode.CONFLICT, "channel trade no already used");
            }
        });
        PaymentOrder payment = get(paymentId);
        if (payment.status() == PaymentStatus.SUCCEEDED) {
            return payment;
        }
        if (payment.amount().compareTo(paidAmount) != 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "paid amount mismatch");
        }
        PaymentOrder succeeded = paymentRepository.save(payment.succeed(channelTradeNo));
        appendLedger(succeeded, LedgerDirection.CREDIT, "PAYMENT", "payment:" + succeeded.paymentId());
        appendEvent(succeeded, EventTypes.PAYMENT_SUCCEEDED);
        boolean orderUpdated = orderClient.payOrder(payment.orderId());
        if (!orderUpdated) {
            return succeeded;
        }
        return paymentRepository.save(succeeded.confirmOrder());
    }

    @Transactional
    public synchronized PaymentOrder refund(long paymentId) {
        PaymentOrder payment = get(paymentId);
        if (payment.status() == PaymentStatus.REFUNDED) {
            return payment;
        }
        if (payment.status() != PaymentStatus.SUCCEEDED && payment.status() != PaymentStatus.REFUNDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "payment cannot be refunded from " + payment.status());
        }
        PaymentOrder refunded = paymentRepository.save(payment.refunding().refunded());
        appendLedger(refunded, LedgerDirection.DEBIT, "REFUND", "refund:" + refunded.paymentId());
        appendEvent(refunded, EventTypes.PAYMENT_REFUNDED);
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
    public synchronized PaymentReconciliationRecord reconcileStatement(long statementId) {
        PaymentChannelStatement statement = settlementRepository.findUnreconciledStatementById(statementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "statement not found"));
        PaymentReconciliationRecord record = buildReconciliationRecord(statement);
        settlementRepository.saveReconciliationIfAbsent(record);
        settlementRepository.markStatementReconciled(statement.statementId());
        return record;
    }

    @Transactional
    public synchronized int reconcileChannelStatements(int limit) {
        return settlementRepository.findUnreconciledStatements(limit).stream()
                .map(statement -> reconcileStatement(statement.statementId())).toList().size();
    }

    public List<PaymentOrder> findSucceededButUnconfirmed(int limit) {
        return paymentRepository.findUnconfirmedByStatus(PaymentStatus.SUCCEEDED, limit);
    }

    @Transactional
    public synchronized PaymentOrder retryOrderConfirmation(long paymentId) {
        PaymentOrder payment = get(paymentId);
        if (payment.status() != PaymentStatus.SUCCEEDED || payment.orderConfirmed()) {
            return payment;
        }
        if (orderClient.payOrder(payment.orderId())) {
            return paymentRepository.save(payment.confirmOrder());
        }
        return payment;
    }

    private PaymentOrder createOnce(String requestId, long orderId, long userId, BigDecimal amount, String channel) {
        if (amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "amount must be positive");
        }
        Instant now = Instant.now();
        return paymentRepository.save(new PaymentOrder(idGenerator.nextId(), requestId, orderId, userId, amount,
                channel, null, PaymentStatus.CREATED, false, now, now));
    }

    private void appendLedger(PaymentOrder payment, LedgerDirection direction, String businessType,
            String referenceId) {
        settlementRepository.saveLedgerIfAbsent(
                new PaymentLedgerEntry(idGenerator.nextId(), payment.paymentId(), payment.orderId(), payment.userId(),
                        direction, payment.amount(), "CNY", businessType, referenceId, Instant.now()));
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
}
