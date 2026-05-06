package com.emall.payment.repository;

import com.emall.payment.domain.PaymentChannelStatement;
import com.emall.payment.domain.PaymentLedgerEntry;
import com.emall.payment.domain.PaymentReconciliationRecord;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryPaymentSettlementRepository implements PaymentSettlementRepository {
    private final ConcurrentMap<String, PaymentLedgerEntry> ledgerByReference = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PaymentChannelStatement> statementsByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PaymentChannelStatement> statementsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PaymentReconciliationRecord> reconciliationByStatement =
            new ConcurrentHashMap<>();

    @Override
    public PaymentLedgerEntry saveLedgerIfAbsent(PaymentLedgerEntry entry) {
        ledgerByReference.putIfAbsent(entry.referenceId(), entry);
        return ledgerByReference.get(entry.referenceId());
    }

    @Override
    public PaymentChannelStatement saveStatementIfAbsent(PaymentChannelStatement statement) {
        String key = statement.channelTradeNo() + "|" + statement.statementType();
        PaymentChannelStatement saved = statementsByKey.putIfAbsent(key, statement);
        PaymentChannelStatement result = saved == null ? statement : saved;
        statementsById.putIfAbsent(result.statementId(), result);
        return result;
    }

    @Override
    public List<PaymentChannelStatement> findUnreconciledStatements(int limit) {
        return statementsById.values().stream()
                .filter(statement -> !statement.reconciled())
                .sorted(Comparator.comparing(PaymentChannelStatement::occurredAt))
                .limit(limit)
                .toList();
    }

    @Override
    public Optional<PaymentChannelStatement> findUnreconciledStatementById(long statementId) {
        return Optional.ofNullable(statementsById.get(statementId))
                .filter(statement -> !statement.reconciled());
    }

    @Override
    public PaymentReconciliationRecord saveReconciliationIfAbsent(PaymentReconciliationRecord record) {
        reconciliationByStatement.putIfAbsent(record.statementId(), record);
        return reconciliationByStatement.get(record.statementId());
    }

    @Override
    public void markStatementReconciled(long statementId) {
        PaymentChannelStatement statement = statementsById.get(statementId);
        if (statement == null) {
            return;
        }
        PaymentChannelStatement reconciled = statement.markReconciled();
        statementsById.put(statementId, reconciled);
        statementsByKey.put(statement.channelTradeNo() + "|" + statement.statementType(), reconciled);
    }
}
