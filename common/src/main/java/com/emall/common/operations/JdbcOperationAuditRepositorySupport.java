package com.emall.common.operations;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class JdbcOperationAuditRepositorySupport implements OperationAuditRepository {
    private final JdbcTemplate jdbcTemplate;

    protected JdbcOperationAuditRepositorySupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OperationAuditRecord save(OperationAuditRecord record) {
        jdbcTemplate.update("""
                INSERT INTO internal_operation_audit
                    (service_name, operation, operator, trace_id, affected, success, message, executed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, record.serviceName(), record.operation(), record.operator(), record.traceId(), record.affected(),
                record.success(), record.message(), Timestamp.from(record.executedAt()));
        return record;
    }
}
