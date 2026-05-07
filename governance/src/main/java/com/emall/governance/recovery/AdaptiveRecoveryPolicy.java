package com.emall.governance.recovery;

import java.time.Duration;

public record AdaptiveRecoveryPolicy(int failureThreshold, int successThreshold, Duration openDuration,
        double halfOpenInitialTrafficRatio, double halfOpenTrafficStep) {
    public static AdaptiveRecoveryPolicy standard() {
        return new AdaptiveRecoveryPolicy(20, 50, Duration.ofSeconds(30), 0.05D, 0.10D);
    }
}
