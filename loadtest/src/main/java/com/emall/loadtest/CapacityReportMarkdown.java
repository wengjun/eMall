package com.emall.loadtest;

import java.util.Locale;

final class CapacityReportMarkdown {
    private CapacityReportMarkdown() {
    }

    static String render(CapacityReport report) {
        StringBuilder text = new StringBuilder();
        text.append("# Capacity report\n\n");
        text.append("## Evidence identity\n\n");
        row(text, "Run ID", report.runId());
        row(text, "Status", report.status());
        row(text, "Environment", report.metadata().environment());
        row(text, "Evidence scope", report.metadata().evidenceScope());
        row(text, "Git commit", report.metadata().gitCommit());
        row(text, "Deployment", report.metadata().deployment());
        row(text, "Target resources", report.metadata().targetResources());
        row(text, "Started", report.startedAt());
        row(text, "Finished", report.finishedAt());
        row(text, "Workers", report.receivedWorkers() + "/" + report.expectedWorkers());
        row(text, "Scenario / pattern", report.scenario() + " / " + report.pattern());

        text.append("\n## Traffic and results\n\n");
        text.append("| Metric | Value |\n| --- | ---: |\n");
        tableRow(text, "Global target QPS", report.globalTargetQps());
        tableRow(text, "Maximum in-flight per worker", report.maxInflightPerWorker());
        tableRow(text, "Attempted", report.metrics().attempted());
        tableRow(text, "Completed", report.metrics().completed());
        tableRow(text, "Success", report.metrics().success());
        tableRow(text, "Failed", report.metrics().failed());
        tableRow(text, "Backpressure rejected", report.metrics().backpressureRejected());
        tableRow(text, "Measured QPS", format(report.metrics().measuredQps()));
        tableRow(text, "Error rate", percent(report.metrics().errorRate()));
        tableRow(text, "P50", micros(report.metrics().p50Micros()));
        tableRow(text, "P95", micros(report.metrics().p95Micros()));
        tableRow(text, "P99", micros(report.metrics().p99Micros()));
        tableRow(text, "Peak in-flight", report.metrics().peakInflight());
        tableRow(text, "Maximum scheduler lag", micros(report.metrics().maxSchedulerLagMicros()));

        text.append("\n## Data and resources\n\n");
        row(text, "Dataset users", Long.toString(report.dataModel().datasetUsers()));
        row(text, "Dataset SKUs", Long.toString(report.dataModel().datasetSkus()));
        row(text, "Active user cardinality", Integer.toString(report.dataModel().activeUserCardinality()));
        row(text, "Active SKU cardinality", Integer.toString(report.dataModel().activeSkuCardinality()));
        row(text, "Hot SKU traffic", report.dataModel().hotSkuPercent() + "%");
        row(text, "Service instances", Integer.toString(report.metadata().serviceInstances()));
        row(text, "Authentication model", report.metadata().authenticationModel());
        row(text, "Generator maximum CPU", percent(report.generator().maxProcessCpuUtilization()));
        row(text, "Generator heap",
                report.generator().totalHeapUsedBytes() + "/" + report.generator().totalHeapMaxBytes() + " bytes");
        row(text, "Generator peak threads", Integer.toString(report.generator().totalPeakThreads()));
        row(text, "Generator bottleneck", Boolean.toString(report.generator().bottleneck()));

        text.append("\n## Saturation metrics\n\n");
        text.append("| Metric | Maximum value |\n| --- | ---: |\n");
        report.saturationMetrics().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> tableRow(text, entry.getKey(), format(entry.getValue())));

        text.append("\n## Horizontal capacity model\n\n");
        row(text, "Safe single-cell QPS", format(report.capacityModel().safeSingleCellQps()));
        row(text, "Projected cells", Integer.toString(report.capacityModel().projectedCells()));
        row(text, "Scaling efficiency", percent(report.capacityModel().scalingEfficiency()));
        row(text, "Projected QPS", format(report.capacityModel().projectedQps()));
        row(text, "Session think time", format(report.capacityModel().sessionThinkSeconds()) + " seconds");
        row(text, "Projected peak concurrency", format(report.capacityModel().projectedPeakConcurrency()));
        row(text, "Target peak concurrency", Long.toString(report.capacityModel().targetPeakConcurrency()));
        row(text, "Target met by model", Boolean.toString(report.capacityModel().targetMet()));
        row(text, "Formula", report.capacityModel().formula());

        text.append("\n## Evidence limitations\n\n");
        if (report.reasons().isEmpty()) {
            text.append("- This run is eligible for the repeated preproduction evidence suite.\n");
        } else {
            report.reasons().forEach(reason -> text.append("- ").append(reason).append("\n"));
        }
        return text.toString();
    }

    static String render(CapacityEvidence evidence) {
        StringBuilder text = new StringBuilder("# Capacity evidence suite\n\n");
        row(text, "Status", evidence.status());
        row(text, "Environment", evidence.environment());
        row(text, "Git commit", evidence.gitCommit());
        row(text, "Eligible runs", Integer.toString(evidence.eligibleRuns()));
        row(text, "Required repetitions per pattern", Integer.toString(evidence.requiredRepetitions()));
        row(text, "Covered patterns", String.join(", ", evidence.coveredPatterns()));
        row(text, "Baseline mean QPS", format(evidence.baselineMeanQps()));
        row(text, "Baseline QPS coefficient of variation", percent(evidence.baselineQpsCoefficientOfVariation()));
        row(text, "Worst P99", micros(evidence.worstP99Micros()));
        row(text, "Capacity model target met", Boolean.toString(evidence.capacityModelTargetMet()));
        text.append("\n## Runs\n\n");
        evidence.runIds().forEach(runId -> text.append("- ").append(runId).append("\n"));
        text.append("\n## Verification findings\n\n");
        if (evidence.reasons().isEmpty()) {
            text.append("- All evidence gates passed.\n");
        } else {
            evidence.reasons().forEach(reason -> text.append("- ").append(reason).append("\n"));
        }
        return text.toString();
    }

    private static void row(StringBuilder text, String name, String value) {
        text.append("- **").append(name).append(":** ").append(value).append("\n");
    }

    private static void tableRow(StringBuilder text, String name, Object value) {
        text.append("| ").append(name).append(" | ").append(value).append(" |\n");
    }

    private static String micros(long value) {
        return format(value / 1_000.0) + " ms";
    }

    private static String percent(double value) {
        return format(value * 100.0) + "%";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
