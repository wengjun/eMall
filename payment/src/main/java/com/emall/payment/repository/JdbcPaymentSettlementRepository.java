package com.emall.payment.repository;

import com.emall.payment.domain.PaymentChannelStatement;
import com.emall.payment.domain.PaymentLedgerEntry;
import com.emall.payment.domain.PaymentReconciliationRecord;
import com.emall.payment.domain.StatementType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcPaymentSettlementRepository implements PaymentSettlementRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcPaymentSettlementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PaymentLedgerEntry saveLedgerIfAbsent(PaymentLedgerEntry entry) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO payment_ledger_entry
                        (ledger_id, payment_id, order_id, user_id, direction, amount, currency, business_type,
                         reference_id, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, entry.ledgerId(), entry.paymentId(), entry.orderId(), entry.userId(), entry.direction().name(),
                    entry.amount(), entry.currency(), entry.businessType(), entry.referenceId(),
                    Timestamp.from(entry.createdAt()));
        } catch (DuplicateKeyException ignored) {
        }
        return entry;
    }

    @Override
    public PaymentChannelStatement saveStatementIfAbsent(PaymentChannelStatement statement) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO payment_channel_statement
                        (statement_id, channel, channel_trade_no, payment_id, amount, statement_type, occurred_at,
                         reconciled, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, statement.statementId(), statement.channel(), statement.channelTradeNo(),
                    statement.paymentId(), statement.amount(), statement.statementType().name(),
                    Timestamp.from(statement.occurredAt()), statement.reconciled(),
                    Timestamp.from(statement.createdAt()));
        } catch (DuplicateKeyException ignored) {
        }
        return statement;
    }

    @Override
    public List<PaymentChannelStatement> findUnreconciledStatements(int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM payment_channel_statement
                WHERE reconciled = false
                ORDER BY occurred_at ASC
                LIMIT ?
                """, this::mapStatement, limit);
    }

    @Override
    public Optional<PaymentChannelStatement> findUnreconciledStatementById(long statementId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject("""
                    SELECT * FROM payment_channel_statement
                    WHERE statement_id = ? AND reconciled = false
                    """, this::mapStatement, statementId));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public PaymentReconciliationRecord saveReconciliationIfAbsent(PaymentReconciliationRecord record) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO payment_reconciliation_record
                        (record_id, statement_id, payment_id, channel_trade_no, statement_type, status, message,
                         created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, record.recordId(), record.statementId(), record.paymentId(), record.channelTradeNo(),
                    record.statementType().name(), record.status().name(), record.message(),
                    Timestamp.from(record.createdAt()));
        } catch (DuplicateKeyException ignored) {
        }
        return record;
    }

    @Override
    public void markStatementReconciled(long statementId) {
        jdbcTemplate.update("UPDATE payment_channel_statement SET reconciled = true WHERE statement_id = ?",
                statementId);
    }

    private PaymentChannelStatement mapStatement(ResultSet rs, int rowNum) throws SQLException {
        return new PaymentChannelStatement(rs.getLong("statement_id"), rs.getString("channel"),
                rs.getString("channel_trade_no"), rs.getLong("payment_id"), rs.getBigDecimal("amount"),
                StatementType.valueOf(rs.getString("statement_type")), rs.getTimestamp("occurred_at").toInstant(),
                rs.getBoolean("reconciled"), rs.getTimestamp("created_at").toInstant());
    }
}
