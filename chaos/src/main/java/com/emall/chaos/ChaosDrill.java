package com.emall.chaos;

import java.time.Duration;
import java.util.List;

public record ChaosDrill(String code, DrillType type, String target, BlastRadius blastRadius, Duration duration,
        List<String> prerequisites, List<String> injectionSteps, List<AbortCondition> abortConditions,
        List<String> recoveryChecks) {
    public ChaosDrill {
        prerequisites = List.copyOf(prerequisites);
        injectionSteps = List.copyOf(injectionSteps);
        abortConditions = List.copyOf(abortConditions);
        recoveryChecks = List.copyOf(recoveryChecks);
    }
}
