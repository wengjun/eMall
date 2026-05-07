package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class CheckoutEndToEndIT {
    @Test
    void shouldCompleteCheckoutAgainstRunningGateway() {
        ProductionHttpGate.assumeEnabled("EMALL_RUN_CHECKOUT_IT");
        String baseUrl = ProductionHttpGate.envOrDefault("EMALL_BASE_URL", "http://localhost:8080");

        assertThatCode(() -> CheckoutSmokeApplication.main(new String[]{baseUrl})).doesNotThrowAnyException();
    }
}
