package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InternalOperationsSecurityIT {
    @Test
    void shouldRejectInvalidInternalOperationTokenAgainstRunningServices() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_INTERNAL_SECURITY_IT");
        String orderUrl = ProductionHttpGate.envOrDefault("EMALL_ORDER_URL", "http://localhost:8084");
        String inventoryUrl = ProductionHttpGate.envOrDefault("EMALL_INVENTORY_URL", "http://localhost:8083");
        String paymentUrl = ProductionHttpGate.envOrDefault("EMALL_PAYMENT_URL", "http://localhost:8086");

        assertThat(ProductionHttpGate.postStatus(orderUrl,
                "/internal/operations/orders/retry-pending?limit=1", "invalid-token")).isEqualTo(403);
        assertThat(ProductionHttpGate.postStatus(inventoryUrl,
                "/internal/operations/inventory/release-expired-reservations?limit=1",
                "invalid-token")).isEqualTo(403);
        assertThat(ProductionHttpGate.postStatus(paymentUrl,
                "/internal/operations/payments/retry-order-confirmation?limit=1",
                "invalid-token")).isEqualTo(403);
    }
}
