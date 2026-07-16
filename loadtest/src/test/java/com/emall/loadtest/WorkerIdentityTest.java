package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkerIdentityTest {
    @Test
    void shouldPartitionRatesAndSequencesWithoutOverlap() {
        Set<Long> sequences = new HashSet<>();
        int totalRate = 0;

        for (int workerIndex = 0; workerIndex < 4; workerIndex++) {
            WorkerIdentity worker = new WorkerIdentity(workerIndex, 4);
            totalRate += worker.localRate(11);
            for (long localSequence = 1; localSequence <= 100; localSequence++) {
                assertThat(sequences.add(worker.globalSequence(localSequence))).isTrue();
            }
        }

        assertThat(totalRate).isEqualTo(11);
        assertThat(sequences).hasSize(400);
    }
}
