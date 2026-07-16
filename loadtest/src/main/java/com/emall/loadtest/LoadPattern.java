package com.emall.loadtest;

import java.time.Duration;
import java.util.List;

enum LoadPattern {
    CONSTANT,
    STEP,
    SPIKE,
    SOAK,
    FAULT_RECOVERY,
    BREAKPOINT;

    static LoadPattern from(String value) {
        return valueOf(value.trim().replace('-', '_').toUpperCase());
    }

    StageDefinition stageAt(Duration elapsed, Duration total, int targetRate) {
        double progress =
                total.isZero() ? 1.0 : Math.min(0.999999, Math.max(0.0, (double) elapsed.toNanos() / total.toNanos()));
        return switch (this) {
            case CONSTANT, SOAK -> stage("steady", targetRate, false, false);
            case STEP -> indexedStage(progress, targetRate, List.of(25, 50, 75, 100), "step", false);
            case BREAKPOINT -> indexedStage(progress, targetRate, List.of(20, 40, 60, 80, 100), "breakpoint", true);
            case SPIKE -> progress < 0.2
                    ? stage("baseline", percent(targetRate, 25), false, false)
                    : progress < 0.4
                            ? stage("spike", targetRate, false, false)
                            : stage("post-spike", percent(targetRate, 50), false, false);
            case FAULT_RECOVERY -> progress < 0.3
                    ? stage("baseline", percent(targetRate, 50), false, false)
                    : progress < 0.6
                            ? stage("fault-window", targetRate, true, false)
                            : stage("recovery", percent(targetRate, 50), false, false);
        };
    }

    List<StageDefinition> stages(int targetRate) {
        return switch (this) {
            case CONSTANT, SOAK -> List.of(stage("steady", targetRate, false, false));
            case STEP -> indexedStages(targetRate, List.of(25, 50, 75, 100), "step", false);
            case BREAKPOINT -> indexedStages(targetRate, List.of(20, 40, 60, 80, 100), "breakpoint", true);
            case SPIKE -> List.of(stage("baseline", percent(targetRate, 25), false, false),
                    stage("spike", targetRate, false, false),
                    stage("post-spike", percent(targetRate, 50), false, false));
            case FAULT_RECOVERY -> List.of(stage("baseline", percent(targetRate, 50), false, false),
                    stage("fault-window", targetRate, true, false),
                    stage("recovery", percent(targetRate, 50), false, false));
        };
    }

    double expectedAverageLocalRate(LoadTestOptions options) {
        return switch (this) {
            case CONSTANT, SOAK -> options.localRate(options.ratePerSecond());
            case STEP -> average(options, List.of(25, 50, 75, 100));
            case SPIKE -> options.localRate(percent(options.ratePerSecond(), 25)) * 0.2
                    + options.localRate(options.ratePerSecond()) * 0.2
                    + options.localRate(percent(options.ratePerSecond(), 50)) * 0.6;
            case FAULT_RECOVERY -> options.localRate(percent(options.ratePerSecond(), 50)) * 0.7
                    + options.localRate(options.ratePerSecond()) * 0.3;
            case BREAKPOINT -> -1.0;
        };
    }

    private StageDefinition indexedStage(double progress, int targetRate, List<Integer> percentages, String prefix,
            boolean checkpoint) {
        int index = Math.min(percentages.size() - 1, (int) (progress * percentages.size()));
        int percentage = percentages.get(index);
        return stage(prefix + '-' + percentage, percent(targetRate, percentage), false, checkpoint);
    }

    private List<StageDefinition> indexedStages(int targetRate, List<Integer> percentages, String prefix,
            boolean checkpoint) {
        return percentages.stream()
                .map(percentage -> stage(prefix + '-' + percentage, percent(targetRate, percentage), false, checkpoint))
                .toList();
    }

    private StageDefinition stage(String name, int rate, boolean faultWindow, boolean checkpoint) {
        return new StageDefinition(name, Math.max(1, rate), faultWindow, checkpoint);
    }

    private int percent(int targetRate, int percentage) {
        return Math.max(1, (int) Math.ceil(targetRate * percentage / 100.0));
    }

    private double average(LoadTestOptions options, List<Integer> percentages) {
        return percentages.stream()
                .mapToInt(percentage -> options.localRate(percent(options.ratePerSecond(), percentage))).average()
                .orElse(0.0);
    }

    record StageDefinition(String name, int globalRate, boolean faultWindow, boolean checkpoint) {
    }
}
