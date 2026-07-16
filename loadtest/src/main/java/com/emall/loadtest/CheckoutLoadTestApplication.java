package com.emall.loadtest;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class CheckoutLoadTestApplication {
    private CheckoutLoadTestApplication() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            printUsage();
            return;
        }
        LoadTestOptions options = LoadTestOptions.from(args);
        LoadTestReportStore reportStore = new LoadTestReportStore(options.reportDirectory());
        if (options.role() == LoadRole.COORDINATOR) {
            CapacityReportCoordinator.Result result = new CapacityReportCoordinator(options, reportStore).aggregate();
            printEvidence(result.evidence(), options.reportDirectory());
            return;
        }

        WorkerReport workerReport;
        Path workerPath;
        try (CheckoutHttpRequestDispatcher dispatcher = new CheckoutHttpRequestDispatcher(options)) {
            if (options.bootstrapData()) {
                dispatcher.bootstrapData();
            }
            workerReport = new LoadExecutionEngine(options, dispatcher).execute();
            workerPath = reportStore.writeWorker(workerReport);
            printWorker(workerReport, workerPath);
        }

        if (options.role() == LoadRole.STANDALONE) {
            CapacityReport capacityReport = new CapacityReportAggregator().aggregate(List.of(workerReport));
            reportStore.writeCapacity(capacityReport);
            CapacityEvidence evidence =
                    new CapacityEvidenceVerifier().verify(List.of(capacityReport), options.requiredRuns());
            reportStore.writeEvidence(evidence);
            printEvidence(evidence, options.reportDirectory());
        }
    }

    private static void printWorker(WorkerReport report, Path reportPath) {
        System.out.printf(Locale.ROOT,
                "%s/%s worker %d of %d completed: requests=%d, success=%d, failed=%d, rejected=%d%n", report.scenario(),
                report.pattern(), report.workerIndex(), report.workerCount(), report.metrics().attempted(),
                report.metrics().success(), report.metrics().failed(), report.metrics().backpressureRejected());
        System.out.printf(Locale.ROOT, "qps=%.2f, errorRate=%.4f, p50=%.2f ms, p95=%.2f ms, p99=%.2f ms%n",
                report.metrics().measuredQps(), report.metrics().errorRate(), report.metrics().p50Micros() / 1_000.0,
                report.metrics().p95Micros() / 1_000.0, report.metrics().p99Micros() / 1_000.0);
        System.out.printf("workerReport=%s, generatorBottleneck=%s%n", reportPath, report.generator().bottleneck());
    }

    private static void printEvidence(CapacityEvidence evidence, Path reportDirectory) {
        System.out.printf("capacityEvidence=%s, status=%s, eligibleRuns=%d%n",
                reportDirectory.resolve("capacity-evidence.json"), evidence.status(), evidence.eligibleRuns());
        if (!evidence.reasons().isEmpty()) {
            System.out.println("Evidence remains unverified: " + String.join("; ", evidence.reasons()));
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar loadtest-*-all.jar [baseUrl ratePerSecond durationSeconds "
                + "maxInflight scenario]");
        System.out.println("Roles: EMALL_LOAD_ROLE=standalone|worker|coordinator");
        System.out.println("Patterns: EMALL_LOAD_PATTERN=constant|step|spike|soak|fault-recovery|breakpoint");
        System.out.println("See docs/capacity-verification.md for distributed execution and evidence settings.");
    }
}
