package com.emall.payment.repository;

import com.emall.common.operations.InMemoryOperationAuditRepositorySupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryOperationAuditRepository extends InMemoryOperationAuditRepositorySupport {
}
