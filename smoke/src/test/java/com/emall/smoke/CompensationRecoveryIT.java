package com.emall.smoke;

import org.junit.jupiter.api.Test;

class CompensationRecoveryIT {
    @Test
    void shouldRunCompensationRecoveryOperationsAgainstRunningServices() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_COMPENSATION_IT");
        String token = ProductionHttpGate.requireEnv("EMALL_INTERNAL_OPERATIONS_TOKEN");

        ProductionHttpGate.post(orderUrl(), "/internal/operations/orders/retry-pending?limit=10", token);
        ProductionHttpGate.post(inventoryUrl(), "/internal/operations/inventory/release-expired-reservations?limit=10",
                token);
        ProductionHttpGate.post(paymentUrl(), "/internal/operations/payments/retry-order-confirmation?limit=10", token);
        ProductionHttpGate.post(orderUrl(), "/internal/operations/outbox/retry-failed?limit=10", token);
        ProductionHttpGate.post(inventoryUrl(), "/internal/operations/outbox/retry-failed?limit=10", token);
        ProductionHttpGate.post(paymentUrl(), "/internal/operations/outbox/retry-failed?limit=10", token);
    }

    private static String orderUrl() {
        return ProductionHttpGate.envOrDefault("EMALL_ORDER_URL", "http://localhost:8084");
    }

    private static String inventoryUrl() {
        return ProductionHttpGate.envOrDefault("EMALL_INVENTORY_URL", "http://localhost:8083");
    }

    private static String paymentUrl() {
        return ProductionHttpGate.envOrDefault("EMALL_PAYMENT_URL", "http://localhost:8086");
    }
}
