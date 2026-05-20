package com.emall.common.operations;

import java.util.Set;

public record OperationSecurityPolicy(Set<String> allowedRoles, Set<String> approvalRequiredOperations,
        boolean approvalRequired) {
    public static OperationSecurityPolicy standard(boolean approvalRequired) {
        return new OperationSecurityPolicy(Set.of("ops-admin", "sre", "release-admin"),
                Set.of("outbox.retry-failed", "payments.ingest-channel-statement",
                        "payments.reconcile-channel-statements", "payments.retry-order-confirmation",
                        "orders.retry-pending"),
                approvalRequired);
    }
}
