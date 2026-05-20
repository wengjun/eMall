package com.emall.common.operations;

public record OperationAuthRequest(String token, String operator, String traceId, String role, String approvalId,
        String sourceIdentity, String parameterDigest) {
}
