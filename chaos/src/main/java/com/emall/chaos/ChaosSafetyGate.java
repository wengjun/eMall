package com.emall.chaos;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ChaosSafetyGate {
    public ChaosSafetyReport validate(ChaosDrill drill) {
        List<String> violations = new ArrayList<>();
        if (drill.duration().compareTo(Duration.ofMinutes(30)) > 0) {
            violations.add("duration exceeds 30 minutes");
        }
        if (drill.blastRadius() == BlastRadius.CROSS_REGION) {
            violations.add("cross-region blast radius requires manual executive approval");
        }
        if (drill.abortConditions().isEmpty()) {
            violations.add("abort conditions are required");
        }
        if (drill.recoveryChecks().isEmpty()) {
            violations.add("recovery checks are required");
        }
        if (drill.prerequisites().stream().noneMatch(item -> item.contains("rollback owner"))) {
            violations.add("rollback owner prerequisite is required");
        }
        return new ChaosSafetyReport(violations.isEmpty(), violations);
    }
}
