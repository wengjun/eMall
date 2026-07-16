package com.emall.loadtest;

import java.util.List;

public record CapacityEvidence(int schemaVersion, String status, int requiredRepetitions, int eligibleRuns,
        List<String> runIds, List<String> coveredPatterns, String environment, String gitCommit, double baselineMeanQps,
        double baselineQpsCoefficientOfVariation, long worstP99Micros, boolean capacityModelTargetMet,
        List<String> reasons) {
}
