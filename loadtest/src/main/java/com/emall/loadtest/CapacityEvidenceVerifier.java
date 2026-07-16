package com.emall.loadtest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class CapacityEvidenceVerifier {
    private static final Set<String> REQUIRED_PATTERNS =
            Set.of("constant", "step", "spike", "soak", "fault_recovery", "breakpoint");
    private static final double MAX_QPS_COEFFICIENT_OF_VARIATION = 0.10;

    CapacityEvidence verify(List<CapacityReport> reports, int requiredRepetitions) {
        if (requiredRepetitions <= 0) {
            throw new IllegalArgumentException("required repetitions must be positive");
        }
        List<CapacityReport> sorted = reports.stream().sorted(Comparator.comparing(CapacityReport::runId)).toList();
        List<String> reasons = new ArrayList<>();
        Set<String> runIds = new HashSet<>();
        for (CapacityReport report : sorted) {
            if (!runIds.add(report.runId())) {
                reasons.add("duplicate run ID: " + report.runId());
            }
            if (!"PREPRODUCTION_RUN_ELIGIBLE".equals(report.status())) {
                reasons.add("run is not eligible: " + report.runId());
            }
        }

        Map<String, List<CapacityReport>> byPattern =
                sorted.stream().collect(Collectors.groupingBy(CapacityReport::pattern));
        Set<String> missingPatterns = new LinkedHashSet<>(REQUIRED_PATTERNS);
        missingPatterns.removeAll(byPattern.keySet());
        if (!missingPatterns.isEmpty()) {
            reasons.add("missing test patterns: " + String.join(", ", missingPatterns));
        }
        REQUIRED_PATTERNS.forEach(pattern -> {
            int count = byPattern.getOrDefault(pattern, List.of()).size();
            if (count < requiredRepetitions) {
                reasons.add(pattern + " has " + count + " runs; " + requiredRepetitions + " required");
            }
        });

        Set<String> commits = sorted.stream().map(report -> report.metadata().gitCommit())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> environments = sorted.stream().map(report -> report.metadata().environment())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (commits.size() != 1) {
            reasons.add("all evidence runs must use the same Git commit");
        }
        if (environments.size() != 1) {
            reasons.add("all evidence runs must use the same preproduction environment");
        }

        List<CapacityReport> baselines = byPattern.getOrDefault("constant", List.of());
        double meanQps = meanQps(baselines);
        double coefficientOfVariation = coefficientOfVariation(baselines, meanQps);
        if (!baselines.isEmpty() && coefficientOfVariation > MAX_QPS_COEFFICIENT_OF_VARIATION) {
            reasons.add("constant baseline QPS coefficient of variation exceeded 10%");
        }
        boolean targetMet = List.of("constant", "step", "soak").stream()
                .flatMap(pattern -> byPattern.getOrDefault(pattern, List.of()).stream())
                .allMatch(report -> report.capacityModel().targetMet());
        if (!targetMet) {
            reasons.add("horizontal capacity model did not meet the peak concurrency target");
        }

        int eligibleRuns =
                (int) sorted.stream().filter(report -> "PREPRODUCTION_RUN_ELIGIBLE".equals(report.status())).count();
        long worstP99 = sorted.stream().mapToLong(report -> report.metrics().p99Micros()).max().orElse(0L);
        return new CapacityEvidence(1, reasons.isEmpty() ? "VERIFIED" : "UNVERIFIED", requiredRepetitions, eligibleRuns,
                sorted.stream().map(CapacityReport::runId).toList(), byPattern.keySet().stream().sorted().toList(),
                environments.size() == 1 ? environments.iterator().next() : "mixed",
                commits.size() == 1 ? commits.iterator().next() : "mixed", meanQps, coefficientOfVariation, worstP99,
                targetMet, List.copyOf(reasons));
    }

    private double meanQps(List<CapacityReport> reports) {
        return reports.stream().mapToDouble(report -> report.metrics().measuredQps()).average().orElse(0.0);
    }

    private double coefficientOfVariation(List<CapacityReport> reports, double mean) {
        if (reports.size() < 2 || mean == 0.0) {
            return 0.0;
        }
        double variance = reports.stream().mapToDouble(report -> {
            double difference = report.metrics().measuredQps() - mean;
            return difference * difference;
        }).sum() / reports.size();
        return Math.sqrt(variance) / mean;
    }
}
