package com.emall.common.operations;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public abstract class MybatisPlusOperationAuditRepositorySupport implements OperationAuditRepository {
    private final BaseMapper<OperationAuditRecordEntity> operationAuditMapper;

    protected MybatisPlusOperationAuditRepositorySupport(BaseMapper<OperationAuditRecordEntity> operationAuditMapper) {
        this.operationAuditMapper = operationAuditMapper;
    }

    @Override
    public OperationAuditRecord save(OperationAuditRecord record) {
        OperationAuditRecordEntity entity = new OperationAuditRecordEntity();
        entity.setServiceName(record.serviceName());
        entity.setOperation(record.operation());
        entity.setOperator(record.operator());
        entity.setTraceId(record.traceId());
        entity.setAffected(record.affected());
        entity.setSuccess(record.success());
        entity.setMessage(record.message());
        entity.setRole(record.role());
        entity.setApprovalId(record.approvalId());
        entity.setSourceIdentity(record.sourceIdentity());
        entity.setParameterDigest(record.parameterDigest());
        entity.setExecutedAt(LocalDateTime.ofInstant(record.executedAt(), ZoneOffset.UTC));
        operationAuditMapper.insert(entity);
        return record;
    }
}
