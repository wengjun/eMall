package com.emall.common.operations;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class InternalOperationAuthorizerTest {
    @Test
    void shouldRequireApprovalForHighRiskOperationWhenEnabled() {
        OperationAuthRequest request =
                new OperationAuthRequest("secret", "alice", "trace-1", "ops-admin", null, "sa:ops", "digest");

        assertThatThrownBy(() -> InternalOperationAuthorizer.authorize("secret", request,
                OperationSecurityPolicy.standard(true), "outbox.retry-failed")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("approval");
    }

    @Test
    void shouldAllowApprovedOperatorRole() {
        OperationAuthRequest request =
                new OperationAuthRequest("secret", "alice", "trace-1", "sre", "APR-1", "sa:ops", "digest");

        assertThatNoException().isThrownBy(() -> InternalOperationAuthorizer.authorize("secret", request,
                OperationSecurityPolicy.standard(true), "outbox.retry-failed"));
    }
}
