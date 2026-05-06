package com.emall.order.repository;

import com.emall.common.operations.JdbcOperationAuditRepositorySupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcOperationAuditRepository extends JdbcOperationAuditRepositorySupport {
    public JdbcOperationAuditRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }
}
