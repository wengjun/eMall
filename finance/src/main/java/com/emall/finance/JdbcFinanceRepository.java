package com.emall.finance;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcFinanceRepository implements FinanceRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcFinanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FinanceAccount saveAccount(FinanceAccount account) {
        jdbcTemplate.update("""
                INSERT INTO finance_account
                    (account_id, account_type, owner_id, currency, balance, frozen_amount, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE balance = VALUES(balance), frozen_amount = VALUES(frozen_amount),
                    updated_at = VALUES(updated_at)
                """, account.accountId(), account.accountType().name(), account.ownerId(), account.currency(),
                account.balance(), account.frozenAmount(), Timestamp.from(account.createdAt()),
                Timestamp.from(account.updatedAt()));
        return account;
    }

    @Override
    public Optional<FinanceAccount> findAccount(long accountId) {
        return jdbcTemplate.query("SELECT * FROM finance_account WHERE account_id = ?", this::mapAccount, accountId)
                .stream().findFirst();
    }

    @Override
    public List<FinanceAccount> findAccounts() {
        return jdbcTemplate.query("SELECT * FROM finance_account", this::mapAccount);
    }

    @Override
    public LedgerEntry saveEntry(LedgerEntry entry) {
        jdbcTemplate.update("""
                INSERT INTO ledger_entry
                    (entry_id, account_id, business_type, business_no, debit_amount, credit_amount, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, entry.entryId(), entry.accountId(), entry.businessType(), entry.businessNo(), entry.debitAmount(),
                entry.creditAmount(), Timestamp.from(entry.createdAt()));
        return entry;
    }

    @Override
    public List<LedgerEntry> findEntries(long accountId) {
        return jdbcTemplate.query("SELECT * FROM ledger_entry WHERE account_id = ?", this::mapEntry, accountId);
    }

    @Override
    public SettlementBatch saveSettlementBatch(SettlementBatch batch) {
        jdbcTemplate.update("""
                INSERT INTO settlement_batch
                    (batch_id, merchant_id, amount, commission_amount, status, settlement_date, created_at,
                    updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, batch.batchId(), batch.merchantId(), batch.amount(), batch.commissionAmount(),
                batch.status().name(), Date.valueOf(batch.settlementDate()), Timestamp.from(batch.createdAt()),
                Timestamp.from(batch.updatedAt()));
        return batch;
    }

    @Override
    public Optional<SettlementBatch> findSettlementBatch(long batchId) {
        return jdbcTemplate
                .query("SELECT * FROM settlement_batch WHERE batch_id = ?", this::mapSettlementBatch, batchId).stream()
                .findFirst();
    }

    @Override
    public List<SettlementBatch> findSettlementBatches(long merchantId) {
        return jdbcTemplate.query("SELECT * FROM settlement_batch WHERE merchant_id = ?", this::mapSettlementBatch,
                merchantId);
    }

    @Override
    public InvoiceDocument saveInvoice(InvoiceDocument invoice) {
        jdbcTemplate.update("""
                INSERT INTO invoice_document
                    (invoice_id, owner_id, amount, tax_no, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, invoice.invoiceId(), invoice.ownerId(), invoice.amount(), invoice.taxNo(), invoice.status().name(),
                Timestamp.from(invoice.createdAt()), Timestamp.from(invoice.updatedAt()));
        return invoice;
    }

    @Override
    public Optional<InvoiceDocument> findInvoice(long invoiceId) {
        return jdbcTemplate.query("SELECT * FROM invoice_document WHERE invoice_id = ?", this::mapInvoice, invoiceId)
                .stream().findFirst();
    }

    @Override
    public ClearingFile saveClearingFile(ClearingFile file) {
        jdbcTemplate.update("""
                INSERT INTO clearing_file
                    (clearing_file_id, channel, clearing_date, amount, balanced, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, file.clearingFileId(), file.channel(), Date.valueOf(file.clearingDate()), file.amount(),
                file.balanced(), Timestamp.from(file.createdAt()));
        return file;
    }

    @Override
    public ChargebackCase saveChargeback(ChargebackCase chargeback) {
        jdbcTemplate.update("""
                INSERT INTO chargeback_case
                    (chargeback_id, payment_id, amount, reason, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, chargeback.chargebackId(), chargeback.paymentId(), chargeback.amount(), chargeback.reason(),
                chargeback.status().name(), Timestamp.from(chargeback.createdAt()),
                Timestamp.from(chargeback.updatedAt()));
        return chargeback;
    }

    @Override
    public Optional<ChargebackCase> findChargeback(long chargebackId) {
        return jdbcTemplate
                .query("SELECT * FROM chargeback_case WHERE chargeback_id = ?", this::mapChargeback, chargebackId)
                .stream().findFirst();
    }

    @Override
    public List<ChargebackCase> findChargebacks() {
        return jdbcTemplate.query("SELECT * FROM chargeback_case", this::mapChargeback);
    }

    private FinanceAccount mapAccount(ResultSet rs, int rowNum) throws SQLException {
        return new FinanceAccount(rs.getLong("account_id"), AccountType.valueOf(rs.getString("account_type")),
                rs.getLong("owner_id"), rs.getString("currency"), rs.getBigDecimal("balance"),
                rs.getBigDecimal("frozen_amount"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private LedgerEntry mapEntry(ResultSet rs, int rowNum) throws SQLException {
        return new LedgerEntry(rs.getLong("entry_id"), rs.getLong("account_id"), rs.getString("business_type"),
                rs.getString("business_no"), rs.getBigDecimal("debit_amount"), rs.getBigDecimal("credit_amount"),
                rs.getTimestamp("created_at").toInstant());
    }

    private SettlementBatch mapSettlementBatch(ResultSet rs, int rowNum) throws SQLException {
        return new SettlementBatch(rs.getLong("batch_id"), rs.getLong("merchant_id"), rs.getBigDecimal("amount"),
                rs.getBigDecimal("commission_amount"), SettlementStatus.valueOf(rs.getString("status")),
                rs.getDate("settlement_date").toLocalDate(), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private InvoiceDocument mapInvoice(ResultSet rs, int rowNum) throws SQLException {
        return new InvoiceDocument(rs.getLong("invoice_id"), rs.getLong("owner_id"), rs.getBigDecimal("amount"),
                rs.getString("tax_no"), InvoiceStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private ChargebackCase mapChargeback(ResultSet rs, int rowNum) throws SQLException {
        return new ChargebackCase(rs.getLong("chargeback_id"), rs.getLong("payment_id"), rs.getBigDecimal("amount"),
                rs.getString("reason"), ChargebackStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
