package com.emall.product.repository;

import com.emall.common.operations.MybatisPlusOperationAuditRepositorySupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusOperationAuditRepository extends MybatisPlusOperationAuditRepositorySupport {
    public MybatisPlusOperationAuditRepository(ProductOperationAuditMapper operationAuditMapper) {
        super(operationAuditMapper);
    }
}
