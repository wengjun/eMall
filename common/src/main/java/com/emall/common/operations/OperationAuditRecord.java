package com.emall.common.operations;

import java.time.Instant;

public record OperationAuditRecord(String serviceName, String operation, String operator, String traceId, int affected,
        boolean success, String message, Instant executedAt) {
    public static OperationAuditRecord success(String serviceName, String operation, String operator, String traceId,
            int affected) {
        return new OperationAuditRecord(serviceName, operation, operator, traceId, affected, true, "OK", Instant.now());
    }
}
