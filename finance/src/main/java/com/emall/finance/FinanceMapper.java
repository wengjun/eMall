package com.emall.finance;

import java.util.List;
import java.util.Map;
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

    @Select("SELECT * FROM finance_account WHERE account_id = #{accountId}")
    Map<String, Object> findAccount(@Param("accountId") long accountId);

    @Select("SELECT * FROM finance_account")
    List<Map<String, Object>> findAccounts();

    @Insert("""
            INSERT INTO ledger_entry
                (entry_id, account_id, business_type, business_no, debit_amount, credit_amount, created_at)
            VALUES (#{entry.entryId}, #{entry.accountId}, #{entry.businessType}, #{entry.businessNo},
                #{entry.debitAmount}, #{entry.creditAmount}, #{entry.createdAt})
            """)
    int saveEntry(@Param("entry") LedgerEntry entry);

    @Select("SELECT * FROM ledger_entry WHERE account_id = #{accountId}")
    List<Map<String, Object>> findEntries(@Param("accountId") long accountId);

    @Insert("""
            INSERT INTO settlement_batch
                (batch_id, merchant_id, amount, commission_amount, status, settlement_date, created_at,
                updated_at)
            VALUES (#{batch.batchId}, #{batch.merchantId}, #{batch.amount}, #{batch.commissionAmount},
                #{batch.status}, #{batch.settlementDate}, #{batch.createdAt}, #{batch.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveSettlementBatch(@Param("batch") SettlementBatch batch);

    @Select("SELECT * FROM settlement_batch WHERE batch_id = #{batchId}")
    Map<String, Object> findSettlementBatch(@Param("batchId") long batchId);

    @Select("SELECT * FROM settlement_batch WHERE merchant_id = #{merchantId}")
    List<Map<String, Object>> findSettlementBatches(@Param("merchantId") long merchantId);

    @Insert("""
            INSERT INTO invoice_document
                (invoice_id, owner_id, amount, tax_no, status, created_at, updated_at)
            VALUES (#{invoice.invoiceId}, #{invoice.ownerId}, #{invoice.amount}, #{invoice.taxNo},
                #{invoice.status}, #{invoice.createdAt}, #{invoice.updatedAt})
            ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
            """)
    int saveInvoice(@Param("invoice") InvoiceDocument invoice);

    @Select("SELECT * FROM invoice_document WHERE invoice_id = #{invoiceId}")
    Map<String, Object> findInvoice(@Param("invoiceId") long invoiceId);

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

    @Select("SELECT * FROM chargeback_case WHERE chargeback_id = #{chargebackId}")
    Map<String, Object> findChargeback(@Param("chargebackId") long chargebackId);

    @Select("SELECT * FROM chargeback_case")
    List<Map<String, Object>> findChargebacks();
}
