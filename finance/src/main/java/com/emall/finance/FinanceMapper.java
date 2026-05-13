package com.emall.finance;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @Select("""
            SELECT account_id, account_type, owner_id, currency, balance, frozen_amount, created_at, updated_at
            FROM finance_account
            WHERE account_id = #{accountId}
            """)
    FinanceAccount findAccount(@Param("accountId") long accountId);

    @Select("""
            SELECT account_id, account_type, owner_id, currency, balance, frozen_amount, created_at, updated_at
            FROM finance_account
            """)
    List<FinanceAccount> findAccounts();

    @Insert("""
            INSERT INTO ledger_entry
                (entry_id, account_id, business_type, business_no, debit_amount, credit_amount, created_at)
            VALUES (#{entry.entryId}, #{entry.accountId}, #{entry.businessType}, #{entry.businessNo},
                #{entry.debitAmount}, #{entry.creditAmount}, #{entry.createdAt})
            """)
    int saveEntry(@Param("entry") LedgerEntry entry);

    @Select("""
            SELECT entry_id, account_id, business_type, business_no, debit_amount, credit_amount, created_at
            FROM ledger_entry
            WHERE account_id = #{accountId}
            """)
    List<LedgerEntry> findEntries(@Param("accountId") long accountId);

    @Insert("""
            INSERT INTO settlement_batch
                (batch_id, merchant_id, amount, commission_amount, status, settlement_date, created_at,
                updated_at)
            VALUES (#{batch.batchId}, #{batch.merchantId}, #{batch.amount}, #{batch.commissionAmount},
                #{batch.status}, #{batch.settlementDate}, #{batch.createdAt}, #{batch.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveSettlementBatch(@Param("batch") SettlementBatch batch);

    @Select("""
            SELECT batch_id, merchant_id, amount, commission_amount, status, settlement_date, created_at, updated_at
            FROM settlement_batch
            WHERE batch_id = #{batchId}
            """)
    SettlementBatch findSettlementBatch(@Param("batchId") long batchId);

    @Select("""
            SELECT batch_id, merchant_id, amount, commission_amount, status, settlement_date, created_at, updated_at
            FROM settlement_batch
            WHERE merchant_id = #{merchantId}
            """)
    List<SettlementBatch> findSettlementBatches(@Param("merchantId") long merchantId);

    @Insert("""
            INSERT INTO invoice_document
                (invoice_id, owner_id, amount, tax_no, status, created_at, updated_at)
            VALUES (#{invoice.invoiceId}, #{invoice.ownerId}, #{invoice.amount}, #{invoice.taxNo},
                #{invoice.status}, #{invoice.createdAt}, #{invoice.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveInvoice(@Param("invoice") InvoiceDocument invoice);

    @Select("""
            SELECT invoice_id, owner_id, amount, tax_no, status, created_at, updated_at
            FROM invoice_document
            WHERE invoice_id = #{invoiceId}
            """)
    InvoiceDocument findInvoice(@Param("invoiceId") long invoiceId);

    @Insert("""
            INSERT INTO clearing_file
                (clearing_file_id, channel, clearing_date, amount, balanced, created_at)
            VALUES (#{file.clearingFileId}, #{file.channel}, #{file.clearingDate}, #{file.amount},
                #{file.balanced}, #{file.createdAt})
            """)
    int saveClearingFile(@Param("file") ClearingFile file);

    @Insert("""
            INSERT INTO chargeback_case
                (chargeback_id, payment_id, amount, reason, status, created_at, updated_at)
            VALUES (#{chargeback.chargebackId}, #{chargeback.paymentId}, #{chargeback.amount},
                #{chargeback.reason}, #{chargeback.status}, #{chargeback.createdAt}, #{chargeback.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveChargeback(@Param("chargeback") ChargebackCase chargeback);

    @Select("""
            SELECT chargeback_id, payment_id, amount, reason, status, created_at, updated_at
            FROM chargeback_case
            WHERE chargeback_id = #{chargebackId}
            """)
    ChargebackCase findChargeback(@Param("chargebackId") long chargebackId);

    @Select("""
            SELECT chargeback_id, payment_id, amount, reason, status, created_at, updated_at
            FROM chargeback_case
            """)
    List<ChargebackCase> findChargebacks();
}
