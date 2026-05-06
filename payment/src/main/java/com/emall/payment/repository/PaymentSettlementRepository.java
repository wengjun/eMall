package com.emall.payment.repository;

import com.emall.payment.domain.PaymentChannelStatement;
import com.emall.payment.domain.PaymentLedgerEntry;
import com.emall.payment.domain.PaymentReconciliationRecord;
import java.util.List;
import java.util.Optional;

public interface PaymentSettlementRepository {
    PaymentLedgerEntry saveLedgerIfAbsent(PaymentLedgerEntry entry);

    PaymentChannelStatement saveStatementIfAbsent(PaymentChannelStatement statement);

    List<PaymentChannelStatement> findUnreconciledStatements(int limit);

    Optional<PaymentChannelStatement> findUnreconciledStatementById(long statementId);

    PaymentReconciliationRecord saveReconciliationIfAbsent(PaymentReconciliationRecord record);

    void markStatementReconciled(long statementId);
}
