package com.emall.finance;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface FinanceMapper {
    @Insert("""
            INSERT INTO finance_account
                (account_id, account_type, owner_id, currency, balance, frozen_amount, created_at, updated_at)
            VALUES (#{account.accountId}, #{account.accountType}, #{account.ownerId}, #{account.currency},
                #{account.balance}, #{account.frozenAmount}, #{account.createdAt}, #{account.updatedAt})
            ON DUPLICATE KEY UPDATE balance = VALUES(balance), frozen_amount = VALUES(frozen_amount),
                updated_at = VALUES(updated_at)
            """)
    int saveAccount(@Param("account") FinanceAccount account);

    @Insert("""
            INSERT INTO settlement_batch
                (batch_id, merchant_id, amount, commission_amount, status, settlement_date, created_at,
                updated_at)
            VALUES (#{batch.batchId}, #{batch.merchantId}, #{batch.amount}, #{batch.commissionAmount},
                #{batch.status}, #{batch.settlementDate}, #{batch.createdAt}, #{batch.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveSettlementBatch(@Param("batch") SettlementBatch batch);

    @Insert("""
            INSERT INTO invoice_document
                (invoice_id, owner_id, amount, tax_no, status, created_at, updated_at)
            VALUES (#{invoice.invoiceId}, #{invoice.ownerId}, #{invoice.amount}, #{invoice.taxNo},
                #{invoice.status}, #{invoice.createdAt}, #{invoice.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveInvoice(@Param("invoice") InvoiceDocument invoice);

    @Insert("""
            INSERT INTO chargeback_case
                (chargeback_id, payment_id, amount, reason, status, created_at, updated_at)
            VALUES (#{chargeback.chargebackId}, #{chargeback.paymentId}, #{chargeback.amount},
                #{chargeback.reason}, #{chargeback.status}, #{chargeback.createdAt}, #{chargeback.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveChargeback(@Param("chargeback") ChargebackCase chargeback);
}
