package com.emall.payment.repository;

import com.emall.payment.domain.PaymentChannelStatement;
import com.emall.payment.domain.PaymentLedgerEntry;
import com.emall.payment.domain.PaymentReconciliationRecord;
import com.emall.payment.domain.PaymentRefundOrder;
import com.emall.payment.domain.PaymentRefundStatus;
import java.util.List;
import java.util.Optional;

public interface PaymentSettlementRepository {
    PaymentRefundOrder saveRefundIfAbsent(PaymentRefundOrder refundOrder);

    boolean updateRefundStatus(long refundId, PaymentRefundStatus expectedStatus, PaymentRefundOrder refundOrder);

    Optional<PaymentRefundOrder> findRefundByRequestId(String requestId);

    PaymentLedgerEntry saveLedgerIfAbsent(PaymentLedgerEntry entry);

    PaymentChannelStatement saveStatementIfAbsent(PaymentChannelStatement statement);

    List<PaymentChannelStatement> findUnreconciledStatements(int limit);

    Optional<PaymentChannelStatement> findUnreconciledStatementById(long statementId);

    PaymentReconciliationRecord saveReconciliationIfAbsent(PaymentReconciliationRecord record);

    void markStatementReconciled(long statementId);
}
