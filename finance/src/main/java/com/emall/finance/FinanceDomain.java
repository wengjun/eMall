package com.emall.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

enum AccountType {
    PLATFORM,
    MERCHANT,
    CUSTOMER_BALANCE,
    FROZEN_FUNDS
}

enum SettlementStatus {
    CREATED,
    APPROVED,
    PAID
}

enum InvoiceStatus {
    ISSUED,
    RECONCILED,
    DISPUTED
}

enum ChargebackStatus {
    OPEN,
    WON,
    LOST
}

record FinanceAccount(long accountId, AccountType accountType, long ownerId, String currency, BigDecimal balance,
        BigDecimal frozenAmount, Instant createdAt, Instant updatedAt) {
    FinanceAccount apply(BigDecimal delta) {
        return new FinanceAccount(accountId, accountType, ownerId, currency, balance.add(delta), frozenAmount,
                createdAt, Instant.now());
    }

    FinanceAccount freeze(BigDecimal amount) {
        return new FinanceAccount(accountId, accountType, ownerId, currency, balance.subtract(amount),
                frozenAmount.add(amount), createdAt, Instant.now());
    }
}

record LedgerEntry(long entryId, long accountId, String businessType, String businessNo, BigDecimal debitAmount,
        BigDecimal creditAmount, Instant createdAt) {
}

record SettlementBatch(long batchId, long merchantId, BigDecimal amount, BigDecimal commissionAmount,
        SettlementStatus status, LocalDate settlementDate, Instant createdAt, Instant updatedAt) {
    SettlementBatch changeStatus(SettlementStatus nextStatus) {
        return new SettlementBatch(batchId, merchantId, amount, commissionAmount, nextStatus, settlementDate, createdAt,
                Instant.now());
    }
}

record InvoiceDocument(long invoiceId, long ownerId, BigDecimal amount, String taxNo, InvoiceStatus status,
        Instant createdAt, Instant updatedAt) {
    InvoiceDocument changeStatus(InvoiceStatus nextStatus) {
        return new InvoiceDocument(invoiceId, ownerId, amount, taxNo, nextStatus, createdAt, Instant.now());
    }
}

record ClearingFile(long clearingFileId, String channel, LocalDate clearingDate, BigDecimal amount, boolean balanced,
        Instant createdAt) {
}

record ChargebackCase(long chargebackId, long paymentId, BigDecimal amount, String reason, ChargebackStatus status,
        Instant createdAt, Instant updatedAt) {
    ChargebackCase changeStatus(ChargebackStatus nextStatus) {
        return new ChargebackCase(chargebackId, paymentId, amount, reason, nextStatus, createdAt, Instant.now());
    }
}

record FinanceSummary(BigDecimal totalBalance, BigDecimal totalFrozen, int settlementBatches, int openChargebacks) {
}
