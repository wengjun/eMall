package com.emall.smoke;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReconciliationIT {
    @Test
    void shouldIngestAndReconcileChannelStatementAgainstRunningPaymentService() throws Exception {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_RECONCILIATION_IT");
        String token = ProductionHttpGate.requireEnv("EMALL_INTERNAL_OPERATIONS_TOKEN");
        long paymentId = Long.parseLong(ProductionHttpGate.requireEnv("EMALL_RECONCILIATION_PAYMENT_ID"));
        String amount = ProductionHttpGate.requireEnv("EMALL_RECONCILIATION_AMOUNT");
        String tradeNo = ProductionHttpGate.requireEnv("EMALL_RECONCILIATION_CHANNEL_TRADE_NO");

        Map<String, Object> statement = Map.of(
                "channel", envOrDefault("EMALL_RECONCILIATION_CHANNEL", "mock"),
                "channelTradeNo", tradeNo,
                "paymentId", paymentId,
                "amount", new BigDecimal(amount),
                "statementType", envOrDefault("EMALL_RECONCILIATION_STATEMENT_TYPE", "PAYMENT"),
                "occurredAt", Instant.now());

        ProductionHttpGate.postJson(paymentUrl(), "/internal/operations/payments/channel-statements",
                statement, token);
        ProductionHttpGate.post(paymentUrl(),
                "/internal/operations/payments/reconcile-channel-statements?limit=10", token);
    }

    private static String paymentUrl() {
        return ProductionHttpGate.envOrDefault("EMALL_PAYMENT_URL", "http://localhost:8086");
    }

    private static String envOrDefault(String envName, String defaultValue) {
        String value = System.getenv(envName);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
