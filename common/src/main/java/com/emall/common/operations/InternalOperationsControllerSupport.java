package com.emall.common.operations;

import com.emall.common.api.ApiResponse;
import java.util.function.IntSupplier;

public abstract class InternalOperationsControllerSupport {
    private final OperationAuditRepository operationAuditRepository;
    private final String serviceName;
    private final String operationsToken;

    protected InternalOperationsControllerSupport(OperationAuditRepository operationAuditRepository,
                                                  String serviceName,
                                                  String operationsToken) {
        this.operationAuditRepository = operationAuditRepository;
        this.serviceName = serviceName;
        this.operationsToken = operationsToken;
    }

    protected ApiResponse<OperationResult> execute(String token, String operator, String traceId,
                                                   String operation, IntSupplier action) {
        InternalOperationAuthorizer.requireAuthorized(operationsToken, token);
        int affected = action.getAsInt();
        operationAuditRepository.save(OperationAuditRecord.success(
                serviceName, operation, operator, traceId, affected));
        return ApiResponse.ok(OperationResult.of(operation, affected));
    }
}
