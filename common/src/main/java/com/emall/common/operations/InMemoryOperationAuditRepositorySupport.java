package com.emall.common.operations;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class InMemoryOperationAuditRepositorySupport implements OperationAuditRepository {
    private final Queue<OperationAuditRecord> records = new ConcurrentLinkedQueue<>();

    @Override
    public OperationAuditRecord save(OperationAuditRecord record) {
        records.add(record);
        return record;
    }
}
