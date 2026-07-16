package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoadTestReportStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldRoundTripWorkerAndWriteMachineAndHumanReadableReports() throws Exception {
        LoadTestReportStore store = new LoadTestReportStore(temporaryDirectory);
        WorkerReport worker = CapacityReportAggregatorTest.worker(0, 1, 10_000L);

        Path workerPath = store.writeWorker(worker);
        CapacityReport report = new CapacityReportAggregator().aggregate(List.of(worker));
        store.writeCapacity(report);
        CapacityEvidence evidence = new CapacityEvidenceVerifier().verify(List.of(report), 2);
        store.writeEvidence(evidence);

        assertThat(store.readWorkers()).containsExactly(worker);
        assertThat(workerPath).exists();
        assertThat(temporaryDirectory.resolve("aggregate-test.capacity.json")).exists();
        assertThat(Files.readString(temporaryDirectory.resolve("aggregate-test.capacity.md"))).contains("Git commit",
                "P50", "Projected peak concurrency", "Evidence limitations");
        assertThat(Files.readString(temporaryDirectory.resolve("capacity-evidence.md"))).contains("UNVERIFIED",
                "Required repetitions per pattern");
    }
}
