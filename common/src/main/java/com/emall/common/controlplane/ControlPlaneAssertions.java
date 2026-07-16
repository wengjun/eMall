package com.emall.common.controlplane;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;

public final class ControlPlaneAssertions {
    private ControlPlaneAssertions() {
    }

    public static ControlPlaneOperation requireSucceeded(ControlPlaneClient client, String module, String resourceType,
            String resourceId) {
        ControlPlaneOperation operation = client.findLatest(module, resourceType, resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                        "external control-plane operation has not been submitted"));
        if (operation.status() != ControlPlaneOperationStatus.SUCCEEDED) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "external control-plane operation is not complete: " + operation.status());
        }
        return operation;
    }
}
