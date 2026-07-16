package com.emall.loadtest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class CapacityReportCoordinator {
    private final LoadTestOptions options;
    private final LoadTestReportStore reportStore;
    private final CapacityReportAggregator aggregator = new CapacityReportAggregator();
    private final CapacityEvidenceVerifier verifier = new CapacityEvidenceVerifier();

    CapacityReportCoordinator(LoadTestOptions options, LoadTestReportStore reportStore) {
        this.options = options;
        this.reportStore = reportStore;
    }

    Result aggregate() {
        List<WorkerReport> workers = selectedWorkers(reportStore.readWorkers());
        if (workers.isEmpty()) {
            throw new IllegalStateException("no worker reports matched the requested run IDs");
        }
        Map<String, List<WorkerReport>> byRun = workers.stream().collect(Collectors.groupingBy(WorkerReport::runId));
        List<CapacityReport> reports = new ArrayList<>();
        Map<String, Double> coordinatorSaturation = SaturationMetricsReader.read(options.saturationFile());
        byRun.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CapacityReport report = aggregator.aggregate(entry.getValue(), coordinatorSaturation);
            reportStore.writeCapacity(report);
            reports.add(report);
        });
        CapacityEvidence evidence = verifier.verify(reports, options.requiredRuns());
        reportStore.writeEvidence(evidence);
        if (options.requireVerifiedEvidence() && !"VERIFIED".equals(evidence.status())) {
            throw new IllegalStateException("capacity evidence gate failed: " + String.join("; ", evidence.reasons()));
        }
        return new Result(List.copyOf(reports), evidence);
    }

    private List<WorkerReport> selectedWorkers(List<WorkerReport> workers) {
        Set<String> selectedRunIds = new LinkedHashSet<>(options.evidenceRunIds());
        if (selectedRunIds.isEmpty() && !"all".equals(options.runId())) {
            selectedRunIds.add(options.runId());
        }
        if (selectedRunIds.isEmpty()) {
            return workers;
        }
        Set<String> available =
                workers.stream().map(WorkerReport::runId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> missing = new LinkedHashSet<>(selectedRunIds);
        missing.removeAll(available);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("missing requested run reports: " + String.join(", ", missing));
        }
        return workers.stream().filter(worker -> selectedRunIds.contains(worker.runId())).toList();
    }

    record Result(List<CapacityReport> reports, CapacityEvidence evidence) {
    }
}
