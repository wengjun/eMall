package com.emall.common.operations;

import com.emall.common.api.ApiResponse;
import java.util.function.IntSupplier;

public abstract class InternalOperationsControllerSupport {
    private final OperationAuditRepository operationAuditRepository;
    private final String serviceName;
    private final String operationsToken;
    private final OperationSecurityPolicy securityPolicy;

    protected InternalOperationsControllerSupport(OperationAuditRepository operationAuditRepository, String serviceName,
            String operationsToken) {
        this(operationAuditRepository, serviceName, operationsToken, false);
    }

    protected InternalOperationsControllerSupport(OperationAuditRepository operationAuditRepository, String serviceName,
            String operationsToken, boolean approvalRequired) {
        this.operationAuditRepository = operationAuditRepository;
        this.serviceName = serviceName;
        this.operationsToken = operationsToken;
        this.securityPolicy = OperationSecurityPolicy.standard(approvalRequired);
    }

    protected ApiResponse<OperationResult> execute(String token, String operator, String traceId, String operation,
            IntSupplier action) {
        return execute(new OperationAuthRequest(token, operator, traceId, "ops-admin", null, null, null), operation,
                action);
    }

    protected ApiResponse<OperationResult> execute(String token, String operator, String traceId, String role,
            String approvalId, String sourceIdentity, String parameterDigest, String operation, IntSupplier action) {
        return execute(
                new OperationAuthRequest(token, operator, traceId, role, approvalId, sourceIdentity, parameterDigest),
                operation, action);
    }

    protected ApiResponse<OperationResult> execute(OperationAuthRequest authRequest, String operation,
            IntSupplier action) {
        InternalOperationAuthorizer.authorize(operationsToken, authRequest, securityPolicy, operation);
        int affected = action.getAsInt();
        operationAuditRepository.save(OperationAuditRecord.success(serviceName, operation, authRequest, affected));
        return ApiResponse.ok(OperationResult.of(operation, affected));
    }
}
