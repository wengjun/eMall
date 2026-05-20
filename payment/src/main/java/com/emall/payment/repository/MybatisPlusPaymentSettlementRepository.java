package com.emall.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.payment.domain.PaymentChannelStatement;
import com.emall.payment.domain.PaymentLedgerEntry;
import com.emall.payment.domain.PaymentReconciliationRecord;
import com.emall.payment.domain.PaymentRefundOrder;
import com.emall.payment.domain.PaymentRefundStatus;
import com.emall.payment.domain.StatementType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusPaymentSettlementRepository implements PaymentSettlementRepository {
    private final PaymentRefundOrderMapper refundOrderMapper;
    private final PaymentLedgerEntryMapper ledgerEntryMapper;
    private final PaymentChannelStatementMapper statementMapper;
    private final PaymentReconciliationRecordMapper reconciliationRecordMapper;

    public MybatisPlusPaymentSettlementRepository(PaymentRefundOrderMapper refundOrderMapper,
            PaymentLedgerEntryMapper ledgerEntryMapper, PaymentChannelStatementMapper statementMapper,
            PaymentReconciliationRecordMapper reconciliationRecordMapper) {
        this.refundOrderMapper = refundOrderMapper;
        this.ledgerEntryMapper = ledgerEntryMapper;
        this.statementMapper = statementMapper;
        this.reconciliationRecordMapper = reconciliationRecordMapper;
    }

    @Override
    public PaymentRefundOrder saveRefundIfAbsent(PaymentRefundOrder refundOrder) {
        try {
            refundOrderMapper.insert(toEntity(refundOrder));
            return refundOrder;
        } catch (DuplicateKeyException ignored) {
            return toRefundDomain(refundOrderMapper
                    .selectOne(new QueryWrapper<PaymentRefundOrderEntity>().eq("request_id", refundOrder.requestId())));
        }
    }

    @Override
    public boolean updateRefundStatus(long refundId, PaymentRefundStatus expectedStatus,
            PaymentRefundOrder refundOrder) {
        PaymentRefundOrderEntity entity = toEntity(refundOrder);
        return refundOrderMapper.update(null,
                new UpdateWrapper<PaymentRefundOrderEntity>().set("channel_refund_no", entity.getChannelRefundNo())
                        .set("status", entity.getStatus()).set("reason", entity.getReason())
                        .set("updated_at", entity.getUpdatedAt()).eq("refund_id", refundId)
                        .eq("status", expectedStatus.name())) == 1;
    }

    @Override
    public Optional<PaymentRefundOrder> findRefundByRequestId(String requestId) {
        return Optional
                .ofNullable(refundOrderMapper
                        .selectOne(new QueryWrapper<PaymentRefundOrderEntity>().eq("request_id", requestId)))
                .map(this::toRefundDomain);
    }

    @Override
    public PaymentLedgerEntry saveLedgerIfAbsent(PaymentLedgerEntry entry) {
        try {
            ledgerEntryMapper.insert(toEntity(entry));
        } catch (DuplicateKeyException ignored) {
        }
        return entry;
    }

    @Override
    public PaymentChannelStatement saveStatementIfAbsent(PaymentChannelStatement statement) {
        try {
            statementMapper.insert(toEntity(statement));
        } catch (DuplicateKeyException ignored) {
        }
        return statement;
    }

    @Override
    public List<PaymentChannelStatement> findUnreconciledStatements(int limit) {
        return statementMapper.selectList(new QueryWrapper<PaymentChannelStatementEntity>().eq("reconciled", false)
                .orderByAsc("occurred_at").last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<PaymentChannelStatement> findUnreconciledStatementById(long statementId) {
        return Optional.ofNullable(statementMapper.selectOne(new QueryWrapper<PaymentChannelStatementEntity>()
                .eq("statement_id", statementId).eq("reconciled", false))).map(this::toDomain);
    }

    @Override
    public PaymentReconciliationRecord saveReconciliationIfAbsent(PaymentReconciliationRecord record) {
        try {
            reconciliationRecordMapper.insert(toEntity(record));
        } catch (DuplicateKeyException ignored) {
        }
        return record;
    }

    @Override
    public void markStatementReconciled(long statementId) {
        statementMapper.update(null, new UpdateWrapper<PaymentChannelStatementEntity>().set("reconciled", true)
                .eq("statement_id", statementId));
    }

    private PaymentLedgerEntryEntity toEntity(PaymentLedgerEntry entry) {
        PaymentLedgerEntryEntity entity = new PaymentLedgerEntryEntity();
        entity.setLedgerId(entry.ledgerId());
        entity.setPaymentId(entry.paymentId());
        entity.setOrderId(entry.orderId());
        entity.setUserId(entry.userId());
        entity.setDirection(entry.direction().name());
        entity.setAccountCode(entry.accountCode());
        entity.setAmount(entry.amount());
        entity.setCurrency(entry.currency());
        entity.setBusinessType(entry.businessType());
        entity.setReferenceId(entry.referenceId());
        entity.setCreatedAt(databaseTime(entry.createdAt()));
        return entity;
    }

    private PaymentRefundOrderEntity toEntity(PaymentRefundOrder refundOrder) {
        PaymentRefundOrderEntity entity = new PaymentRefundOrderEntity();
        entity.setRefundId(refundOrder.refundId());
        entity.setPaymentId(refundOrder.paymentId());
        entity.setRequestId(refundOrder.requestId());
        entity.setChannel(refundOrder.channel());
        entity.setChannelRefundNo(refundOrder.channelRefundNo());
        entity.setAmount(refundOrder.amount());
        entity.setStatus(refundOrder.status().name());
        entity.setReason(refundOrder.reason());
        entity.setCreatedAt(databaseTime(refundOrder.createdAt()));
        entity.setUpdatedAt(databaseTime(refundOrder.updatedAt()));
        return entity;
    }

    private PaymentRefundOrder toRefundDomain(PaymentRefundOrderEntity entity) {
        return new PaymentRefundOrder(entity.getRefundId(), entity.getPaymentId(), entity.getRequestId(),
                entity.getChannel(), entity.getChannelRefundNo(), entity.getAmount(),
                PaymentRefundStatus.valueOf(entity.getStatus()), entity.getReason(), domainTime(entity.getCreatedAt()),
                domainTime(entity.getUpdatedAt()));
    }

    private PaymentChannelStatementEntity toEntity(PaymentChannelStatement statement) {
        PaymentChannelStatementEntity entity = new PaymentChannelStatementEntity();
        entity.setStatementId(statement.statementId());
        entity.setChannel(statement.channel());
        entity.setChannelTradeNo(statement.channelTradeNo());
        entity.setPaymentId(statement.paymentId());
        entity.setAmount(statement.amount());
        entity.setStatementType(statement.statementType().name());
        entity.setOccurredAt(databaseTime(statement.occurredAt()));
        entity.setReconciled(statement.reconciled());
        entity.setCreatedAt(databaseTime(statement.createdAt()));
        return entity;
    }

    private PaymentChannelStatement toDomain(PaymentChannelStatementEntity entity) {
        return new PaymentChannelStatement(entity.getStatementId(), entity.getChannel(), entity.getChannelTradeNo(),
                entity.getPaymentId(), entity.getAmount(), StatementType.valueOf(entity.getStatementType()),
                domainTime(entity.getOccurredAt()), entity.getReconciled(), domainTime(entity.getCreatedAt()));
    }

    private PaymentReconciliationRecordEntity toEntity(PaymentReconciliationRecord record) {
        PaymentReconciliationRecordEntity entity = new PaymentReconciliationRecordEntity();
        entity.setRecordId(record.recordId());
        entity.setStatementId(record.statementId());
        entity.setPaymentId(record.paymentId());
        entity.setChannelTradeNo(record.channelTradeNo());
        entity.setStatementType(record.statementType().name());
        entity.setStatus(record.status().name());
        entity.setMessage(record.message());
        entity.setCreatedAt(databaseTime(record.createdAt()));
        return entity;
    }

    private LocalDateTime databaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant domainTime(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC);
    }
}
