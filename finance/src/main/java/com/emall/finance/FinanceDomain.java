package com.emall.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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

@TableName("finance_account")
record FinanceAccount(@TableId(value = "account_id", type = IdType.INPUT) long accountId, AccountType accountType,
        long ownerId, String currency, BigDecimal balance, BigDecimal frozenAmount, Instant createdAt,
        Instant updatedAt) {
    FinanceAccount apply(BigDecimal delta) {
        return new FinanceAccount(accountId, accountType, ownerId, currency, balance.add(delta), frozenAmount,
                createdAt, Instant.now());
    }

    FinanceAccount freeze(BigDecimal amount) {
        return new FinanceAccount(accountId, accountType, ownerId, currency, balance.subtract(amount),
                frozenAmount.add(amount), createdAt, Instant.now());
    }
}

@TableName("ledger_entry")
record LedgerEntry(@TableId(value = "entry_id", type = IdType.INPUT) long entryId, long accountId,
        String businessType, String businessNo, BigDecimal debitAmount, BigDecimal creditAmount, Instant createdAt) {
}

@TableName("settlement_batch")
record SettlementBatch(@TableId(value = "batch_id", type = IdType.INPUT) long batchId, long merchantId,
        BigDecimal amount, BigDecimal commissionAmount, SettlementStatus status, LocalDate settlementDate,
        Instant createdAt, Instant updatedAt) {
    SettlementBatch changeStatus(SettlementStatus nextStatus) {
        return new SettlementBatch(batchId, merchantId, amount, commissionAmount, nextStatus, settlementDate, createdAt,
                Instant.now());
    }
}

@TableName("invoice_document")
record InvoiceDocument(@TableId(value = "invoice_id", type = IdType.INPUT) long invoiceId, long ownerId,
        BigDecimal amount, String taxNo, InvoiceStatus status, Instant createdAt, Instant updatedAt) {
    InvoiceDocument changeStatus(InvoiceStatus nextStatus) {
        return new InvoiceDocument(invoiceId, ownerId, amount, taxNo, nextStatus, createdAt, Instant.now());
    }
}

@TableName("clearing_file")
record ClearingFile(@TableId(value = "clearing_file_id", type = IdType.INPUT) long clearingFileId, String channel,
        LocalDate clearingDate, BigDecimal amount, boolean balanced, Instant createdAt) {
}

@TableName("chargeback_case")
record ChargebackCase(@TableId(value = "chargeback_id", type = IdType.INPUT) long chargebackId, long paymentId,
        BigDecimal amount, String reason, ChargebackStatus status, Instant createdAt, Instant updatedAt) {
    ChargebackCase changeStatus(ChargebackStatus nextStatus) {
        return new ChargebackCase(chargebackId, paymentId, amount, reason, nextStatus, createdAt, Instant.now());
    }
}

record FinanceSummary(BigDecimal totalBalance, BigDecimal totalFrozen, int settlementBatches, int openChargebacks) {
}
