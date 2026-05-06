package com.emall.merchant.repository;

import com.emall.merchant.domain.CommissionRule;
import com.emall.merchant.domain.Invoice;
import com.emall.merchant.domain.InvoiceStatus;
import com.emall.merchant.domain.Merchant;
import com.emall.merchant.domain.MerchantStatus;
import com.emall.merchant.domain.Settlement;
import com.emall.merchant.domain.SettlementStatus;
import com.emall.merchant.domain.Store;
import com.emall.merchant.domain.StoreStatus;
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
public class JdbcMerchantRepository implements MerchantRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcMerchantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Merchant saveMerchant(Merchant merchant) {
        jdbcTemplate.update("""
                INSERT INTO merchant (merchant_id, name, contact_email, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), contact_email = VALUES(contact_email),
                    status = VALUES(status), updated_at = VALUES(updated_at)
                """,
                merchant.merchantId(), merchant.name(), merchant.contactEmail(), merchant.status().name(),
                Timestamp.from(merchant.createdAt()), Timestamp.from(merchant.updatedAt()));
        return merchant;
    }

    @Override
    public Optional<Merchant> findMerchant(long merchantId) {
        return jdbcTemplate.query("SELECT * FROM merchant WHERE merchant_id = ?", this::mapMerchant, merchantId)
                .stream()
                .findFirst();
    }

    @Override
    public Store saveStore(Store store) {
        jdbcTemplate.update("""
                INSERT INTO merchant_store
                    (store_id, merchant_id, name, description, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description),
                    status = VALUES(status), updated_at = VALUES(updated_at)
                """,
                store.storeId(), store.merchantId(), store.name(), store.description(), store.status().name(),
                Timestamp.from(store.createdAt()), Timestamp.from(store.updatedAt()));
        return store;
    }

    @Override
    public Optional<Store> findStore(long storeId) {
        return jdbcTemplate.query("SELECT * FROM merchant_store WHERE store_id = ?", this::mapStore, storeId)
                .stream()
                .findFirst();
    }

    @Override
    public List<Store> findStoresByMerchant(long merchantId) {
        return jdbcTemplate.query("""
                SELECT * FROM merchant_store
                WHERE merchant_id = ?
                ORDER BY created_at DESC
                """, this::mapStore, merchantId);
    }

    @Override
    public CommissionRule saveCommissionRule(CommissionRule rule) {
        jdbcTemplate.update("""
                UPDATE merchant_commission_rule
                SET active = false, updated_at = ?
                WHERE merchant_id = ? AND active = true
                """, Timestamp.from(rule.updatedAt()), rule.merchantId());
        jdbcTemplate.update("""
                INSERT INTO merchant_commission_rule
                    (rule_id, merchant_id, rate, active, effective_from, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE rate = VALUES(rate), active = VALUES(active),
                    effective_from = VALUES(effective_from), updated_at = VALUES(updated_at)
                """,
                rule.ruleId(), rule.merchantId(), rule.rate(), rule.active(), Timestamp.from(rule.effectiveFrom()),
                Timestamp.from(rule.createdAt()), Timestamp.from(rule.updatedAt()));
        return rule;
    }

    @Override
    public Optional<CommissionRule> findActiveCommissionRule(long merchantId) {
        return jdbcTemplate.query("""
                SELECT * FROM merchant_commission_rule
                WHERE merchant_id = ? AND active = true
                ORDER BY effective_from DESC
                LIMIT 1
                """, this::mapCommissionRule, merchantId)
                .stream()
                .findFirst();
    }

    @Override
    public Settlement saveSettlement(Settlement settlement) {
        jdbcTemplate.update("""
                INSERT INTO merchant_settlement
                    (settlement_id, merchant_id, gross_amount, commission_amount, net_amount, status,
                     period_start, period_end, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """,
                settlement.settlementId(), settlement.merchantId(), settlement.grossAmount(),
                settlement.commissionAmount(), settlement.netAmount(), settlement.status().name(),
                Timestamp.from(settlement.periodStart()), Timestamp.from(settlement.periodEnd()),
                Timestamp.from(settlement.createdAt()), Timestamp.from(settlement.updatedAt()));
        return settlement;
    }

    @Override
    public Optional<Settlement> findSettlement(long settlementId) {
        return jdbcTemplate.query("SELECT * FROM merchant_settlement WHERE settlement_id = ?",
                        this::mapSettlement, settlementId)
                .stream()
                .findFirst();
    }

    @Override
    public List<Settlement> findSettlementsByMerchant(long merchantId) {
        return jdbcTemplate.query("""
                SELECT * FROM merchant_settlement
                WHERE merchant_id = ?
                ORDER BY created_at DESC
                """, this::mapSettlement, merchantId);
    }

    @Override
    public Invoice saveInvoice(Invoice invoice) {
        jdbcTemplate.update("""
                INSERT INTO merchant_invoice
                    (invoice_id, settlement_id, merchant_id, amount, status, invoice_title, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """,
                invoice.invoiceId(), invoice.settlementId(), invoice.merchantId(), invoice.amount(),
                invoice.status().name(), invoice.invoiceTitle(), Timestamp.from(invoice.createdAt()),
                Timestamp.from(invoice.updatedAt()));
        return invoice;
    }

    @Override
    public List<Invoice> findInvoicesByMerchant(long merchantId) {
        return jdbcTemplate.query("""
                SELECT * FROM merchant_invoice
                WHERE merchant_id = ?
                ORDER BY created_at DESC
                """, this::mapInvoice, merchantId);
    }

    private Merchant mapMerchant(ResultSet rs, int rowNum) throws SQLException {
        return new Merchant(
                rs.getLong("merchant_id"),
                rs.getString("name"),
                rs.getString("contact_email"),
                MerchantStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private Store mapStore(ResultSet rs, int rowNum) throws SQLException {
        return new Store(
                rs.getLong("store_id"),
                rs.getLong("merchant_id"),
                rs.getString("name"),
                rs.getString("description"),
                StoreStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private CommissionRule mapCommissionRule(ResultSet rs, int rowNum) throws SQLException {
        return new CommissionRule(
                rs.getLong("rule_id"),
                rs.getLong("merchant_id"),
                rs.getBigDecimal("rate"),
                rs.getBoolean("active"),
                rs.getTimestamp("effective_from").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private Settlement mapSettlement(ResultSet rs, int rowNum) throws SQLException {
        return new Settlement(
                rs.getLong("settlement_id"),
                rs.getLong("merchant_id"),
                rs.getBigDecimal("gross_amount"),
                rs.getBigDecimal("commission_amount"),
                rs.getBigDecimal("net_amount"),
                SettlementStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("period_start").toInstant(),
                rs.getTimestamp("period_end").toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private Invoice mapInvoice(ResultSet rs, int rowNum) throws SQLException {
        return new Invoice(
                rs.getLong("invoice_id"),
                rs.getLong("settlement_id"),
                rs.getLong("merchant_id"),
                rs.getBigDecimal("amount"),
                InvoiceStatus.valueOf(rs.getString("status")),
                rs.getString("invoice_title"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
