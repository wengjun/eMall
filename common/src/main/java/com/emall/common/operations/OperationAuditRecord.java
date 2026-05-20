package com.emall.common.operations;

import java.time.Instant;

public record OperationAuditRecord(String serviceName, String operation, String operator, String traceId, int affected,
        boolean success, String message, Instant executedAt, String role, String approvalId, String sourceIdentity,
        String parameterDigest) {
    public static OperationAuditRecord success(String serviceName, String operation, String operator, String traceId,
            int affected) {
        return new OperationAuditRecord(serviceName, operation, operator, traceId, affected, true, "OK", Instant.now(),
                "ops-admin", null, null, null);
    }

    public static OperationAuditRecord success(String serviceName, String operation, OperationAuthRequest authRequest,
            int affected) {
        return new OperationAuditRecord(serviceName, operation, authRequest.operator(), authRequest.traceId(), affected,
                true, "OK", Instant.now(), authRequest.role(), authRequest.approvalId(), authRequest.sourceIdentity(),
                authRequest.parameterDigest());
    }
}
