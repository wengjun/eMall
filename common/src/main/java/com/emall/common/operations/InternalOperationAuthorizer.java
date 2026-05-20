package com.emall.common.operations;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class InternalOperationAuthorizer {
    private InternalOperationAuthorizer() {
    }

    public static void requireAuthorized(String expectedToken, String actualToken) {
        authorize(expectedToken, new OperationAuthRequest(actualToken, "unknown", null, "ops-admin", null, null, null),
                OperationSecurityPolicy.standard(false), "legacy");
    }

    public static void authorize(String expectedToken, OperationAuthRequest request, OperationSecurityPolicy policy,
            String operation) {
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "internal operations token is not configured");
        }
        if (request.token() == null || request.token().isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "internal operations token is required");
        }
        byte[] expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        byte[] actual = request.token().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "internal operations token is invalid");
        }
        if (request.role() == null || !policy.allowedRoles().contains(request.role())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "internal operations role is not allowed");
        }
        if (policy.approvalRequired() && policy.approvalRequiredOperations().contains(operation)
                && (request.approvalId() == null || request.approvalId().isBlank())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "internal operation approval is required");
        }
    }
}
